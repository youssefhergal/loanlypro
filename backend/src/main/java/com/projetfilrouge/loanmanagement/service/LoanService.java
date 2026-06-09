package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEvent;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.LoanDocument;
import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.entity.LoanEventActorType;
import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationEventRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationSpecifications;
import com.projetfilrouge.loanmanagement.repository.LoanDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminApplicationListQuery;
import com.projetfilrouge.loanmanagement.web.dto.request.AdminLoanListSort;
import com.projetfilrouge.loanmanagement.web.dto.request.CancelLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.LoanSubmittedUpdateDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ProposeOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectOfferRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.RejectLoanRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.request.ValidateDocumentRequestDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminAdvisorOptionDto;
import com.projetfilrouge.loanmanagement.web.dto.response.AdminLoanListSummaryDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentReviewResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanHistoryEventResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LoanResponseDto;
import org.springframework.data.jpa.domain.Specification;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CONSEILLER = "ROLE_CONSEILLER";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final int MAX_REFERENCE_GENERATION_ATTEMPTS = 10;
    private static final int MAX_OTHER_DOCUMENTS = 2;
    private static final int MAX_DOCUMENT_DISPLAY_NAME_LENGTH = 120;
    private static final Set<LoanDocumentType> REQUIRED_DOCUMENT_TYPES = EnumSet.of(
            LoanDocumentType.IDENTITY,
            LoanDocumentType.PAYSLIPS,
            LoanDocumentType.TAX_NOTICE,
            LoanDocumentType.BANK_STATEMENTS,
            LoanDocumentType.PROOF_OF_ADDRESS
    );
    private static final BigDecimal SYSTEM_INTEREST_RATE = new BigDecimal("3.85");
    private static final String MSG_DOSSIER_INTROUVABLE = "Dossier introuvable";
    private static final String MSG_ACCES_REFUSE = "Accès refusé";
    private static final String MSG_ACCES_REFUSE_DEMANDE = "Accès refusé à cette demande";
    private static final String PAYLOAD_DOCUMENT_TYPE = "documentType";
    private static final String PAYLOAD_COMMENT = "comment";

    private final LoanApplicationRepository loanRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanApplicationEventRepository eventRepository;
    private final UserRepository userRepository;
    private final LoanApplicationHistoryService historyService;
    private final DocumentReviewService documentReviewService;
    private final LoanDocumentStorageService documentStorage;

    @Transactional
    public LoanResponseDto createApplication(LoanRequestDto request, String currentUserEmail) {
        User applicant = getRequiredUser(currentUserEmail);

        LoanApplication loanApplication = LoanApplication.builder()
                .reference(generateUniqueReference())
                .applicant(applicant)
                .status(LoanApplicationStatus.DRAFT)
                .build();
        applyRequestToLoan(loanApplication, request);

        LoanApplication saved = loanRepository.save(loanApplication);
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.APPLICATION_CREATED,
                LoanEventActorType.CLIENT,
                applicant.getEmail(),
                displayName(applicant),
                Map.of("reference", saved.getReference())
        );
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> getAllApplications(String currentUserEmail, LoanApplicationStatus status, int page, int size) {
        User currentUser = getRequiredUser(currentUserEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoanApplication> loans;

        if (hasRole(currentUser, ROLE_ADMIN)) {
            loans = status == null
                    ? loanRepository.findAll(pageable)
                    : loanRepository.findByStatus(status, pageable);
        } else if (hasRole(currentUser, ROLE_CONSEILLER)) {
            loans = status == null
                    ? loanRepository.findVisibleToAdvisor(currentUser.getId(), pageable)
                    : loanRepository.findVisibleToAdvisorAndStatus(currentUser.getId(), status, pageable);
        } else {
            loans = status == null
                    ? loanRepository.findByApplicantEmail(currentUserEmail, pageable)
                    : loanRepository.findByApplicantEmailAndStatus(currentUserEmail, status, pageable);
        }

        return loans.map(this::mapToResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> getAdminApplications(String currentUserEmail, AdminApplicationListQuery query) {
        User currentUser = getRequiredUser(currentUserEmail);
        ensureAdmin(currentUser);

        LoanApplicationStatus statusFilter = query.unassignedOnly() ? null : query.status();
        Specification<LoanApplication> spec = LoanApplicationSpecifications.adminList(
                query.search(),
                query.advisorId(),
                query.unassignedOnly(),
                statusFilter
        );
        Pageable pageable = PageRequest.of(query.page(), query.size(), adminListSort(query.sort()));
        return loanRepository.findAll(spec, pageable).map(this::mapToResponseDto);
    }

    @Transactional(readOnly = true)
    public AdminLoanListSummaryDto getAdminListSummary(String currentUserEmail, String search) {
        User currentUser = getRequiredUser(currentUserEmail);
        ensureAdmin(currentUser);

        long totalCount = loanRepository.count(
                LoanApplicationSpecifications.adminList(search, null, false, null)
        );
        long unassignedCount = loanRepository.count(
                LoanApplicationSpecifications.adminList(search, null, true, null)
        );

        Map<LoanApplicationStatus, Long> statusCounts = new EnumMap<>(LoanApplicationStatus.class);
        for (LoanApplicationStatus applicationStatus : LoanApplicationStatus.values()) {
            if (applicationStatus == LoanApplicationStatus.DRAFT) {
                continue;
            }
            statusCounts.put(
                    applicationStatus,
                    loanRepository.count(
                            LoanApplicationSpecifications.adminList(search, null, false, applicationStatus)
                    )
            );
        }

        List<AdminAdvisorOptionDto> advisors = userRepository.findAllConseillers().stream()
                .map(user -> AdminAdvisorOptionDto.builder()
                        .id(user.getId())
                        .name(displayName(user))
                        .build())
                .toList();

        return AdminLoanListSummaryDto.builder()
                .totalCount(totalCount)
                .unassignedCount(unassignedCount)
                .statusCounts(statusCounts)
                .advisors(advisors)
                .build();
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getApplicationById(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de prêt introuvable avec l'identifiant : " + id));
        ensureCanAccessLoan(loan, currentUser);
        return mapToResponseDto(loan);
    }

    @Transactional
    public LoanResponseDto updateDraftApplication(Long id, LoanRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanEditDraft(loan);

        applyRequestToLoan(loan, request);

        return mapToResponseDto(loanRepository.save(loan));
    }

    @Transactional
    public void deleteApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        ensureCanAccessLoan(loan, currentUser);

        if (hasRole(currentUser, ROLE_CLIENT) && loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Suppression impossible : un client ne peut supprimer qu'un dossier en brouillon (DRAFT)."
            );
        }

        Long loanId = loan.getId();
        List<LoanDocument> documents = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId);
        for (LoanDocument document : documents) {
            documentStorage.deleteFile(document.getStoragePath());
        }
        loanDocumentRepository.deleteAll(documents);
        documentStorage.deleteLoanStorageDirectory(loanId);
        eventRepository.deleteByLoanApplicationId(loanId);
        loanRepository.delete(loan);
    }

    @Transactional
    public LoanResponseDto updateSubmittedApplication(Long id, LoanSubmittedUpdateDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!(hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_CONSEILLER))) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
        }

        if (loan.getStatus() != LoanApplicationStatus.SUBMITTED
                && loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Mise à jour impossible : seul un dossier soumis ou en analyse peut être modifié."
            );
        }

        User newlyAssignedAdvisor = null;
        boolean recordAdvisorAssignment = false;
        if (request.getAssignedAdvisorId() != null && loan.getAssignedAdvisor() == null) {
            newlyAssignedAdvisor = userRepository.findById(request.getAssignedAdvisorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conseiller introuvable"));
            loan.setAssignedAdvisor(newlyAssignedAdvisor);
            recordAdvisorAssignment = true;
        }
        if (request.getApprovedAmount() != null) {
            loan.setApprovedAmount(request.getApprovedAmount());
        }
        if (request.getApprovedDurationMonths() != null) {
            loan.setApprovedDurationMonths(request.getApprovedDurationMonths());
        }
        if (request.getInterestRate() != null) {
            loan.setInterestRate(request.getInterestRate());
        }

        LoanApplication saved = loanRepository.save(loan);
        if (recordAdvisorAssignment && newlyAssignedAdvisor != null) {
            historyService.recordEvent(
                    saved,
                    LoanApplicationEventType.ADVISOR_ASSIGNED,
                    actorTypeFor(currentUser),
                    currentUser.getEmail(),
                    displayName(currentUser),
                    Map.of("advisorName", displayName(newlyAssignedAdvisor))
            );
        }
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto submitApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanSubmit(loan);
        ensureRequiredDocumentsPresent(loan.getId());

        loan.setStatus(LoanApplicationStatus.SUBMITTED);
        if (loan.getSubmittedAt() == null) {
            loan.setSubmittedAt(Instant.now());
        }

        LoanApplication saved = loanRepository.save(loan);
        recordInitialDocumentsBundle(saved, currentUser, saved.getSubmittedAt());
        documentReviewService.initializePendingReviews(saved);
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.APPLICATION_SUBMITTED,
                LoanEventActorType.CLIENT,
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of("reference", saved.getReference()),
                saved.getSubmittedAt()
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto startReview(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!(hasRole(currentUser, ROLE_CONSEILLER) || hasRole(currentUser, ROLE_ADMIN))) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE);
        }

        if (loan.getStatus() != LoanApplicationStatus.SUBMITTED
                && loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Analyse impossible : le dossier doit être SUBMITTED ou UNDER_REVIEW."
            );
        }

        LoanApplicationStatus previousStatus = loan.getStatus();
        if (previousStatus == LoanApplicationStatus.UNDER_REVIEW) {
            return mapToResponseDto(loan);
        }

        boolean assigned = false;
        if (hasRole(currentUser, ROLE_CONSEILLER)) {
            assigned = assignAdvisorIfNeeded(loan, currentUser);
        }

        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        LoanApplication saved = loanRepository.save(loan);

        if (assigned) {
            historyService.recordEvent(
                    saved,
                    LoanApplicationEventType.ADVISOR_ASSIGNED,
                    actorTypeFor(currentUser),
                    currentUser.getEmail(),
                    displayName(currentUser),
                    Map.of("advisorName", displayName(currentUser))
            );
        }

        if (previousStatus == LoanApplicationStatus.SUBMITTED) {
            historyService.recordEvent(
                    saved,
                    LoanApplicationEventType.REVIEW_STARTED,
                    actorTypeFor(currentUser),
                    currentUser.getEmail(),
                    displayName(currentUser),
                    Map.of()
            );
        }
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanHistoryEventResponseDto rejectDocument(
            Long id,
            RejectDocumentRequestDto request,
            String currentUserEmail
    ) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!(hasRole(currentUser, ROLE_CONSEILLER) || hasRole(currentUser, ROLE_ADMIN))) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE);
        }

        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Rejet de document impossible : le dossier doit être en analyse (UNDER_REVIEW)."
            );
        }

        documentReviewService.markRejected(loan, request.getDocumentType(), request.getComment());

        LoanApplicationEvent event = historyService.recordEvent(
                loan,
                LoanApplicationEventType.DOCUMENT_REJECTED,
                actorTypeFor(currentUser),
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(
                        PAYLOAD_DOCUMENT_TYPE, request.getDocumentType().name(),
                        PAYLOAD_COMMENT, request.getComment()
                )
        );

        return historyService.mapEventToDisplayDto(event);
    }

    @Transactional
    public LoanHistoryEventResponseDto validateDocument(
            Long id,
            ValidateDocumentRequestDto request,
            String currentUserEmail
    ) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!(hasRole(currentUser, ROLE_CONSEILLER) || hasRole(currentUser, ROLE_ADMIN))) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE);
        }

        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Validation de document impossible : le dossier doit être en analyse (UNDER_REVIEW)."
            );
        }

        ensureDocumentFilePresent(loan.getId(), request.getDocumentType());

        documentReviewService.markValidated(loan, request.getDocumentType());

        LoanApplicationEvent event = historyService.recordEvent(
                loan,
                LoanApplicationEventType.DOCUMENT_VALIDATED,
                actorTypeFor(currentUser),
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(PAYLOAD_DOCUMENT_TYPE, request.getDocumentType().name())
        );

        return historyService.mapEventToDisplayDto(event);
    }

    @Transactional(readOnly = true)
    public List<LoanHistoryEventResponseDto> getApplicationHistory(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        return historyService.getHistory(id);
    }

    @Transactional
    public LoanResponseDto proposeCounterOffer(Long id, ProposeOfferRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        if (!(hasRole(currentUser, ROLE_CONSEILLER) || hasRole(currentUser, ROLE_ADMIN))) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE);
        }

        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureAdvisorCanManage(loan, currentUser);

        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Contre-offre impossible : le dossier doit être en analyse (UNDER_REVIEW)."
            );
        }

        ensureRequiredDocumentsPresent(loan.getId());
        documentReviewService.ensureRequiredDocumentsReadyForDecision(loan.getId(), "Contre-offre impossible");

        loan.setApprovedAmount(request.getApprovedAmount());
        loan.setApprovedDurationMonths(request.getApprovedDurationMonths());
        loan.setInterestRate(request.getInterestRate());
        loan.setOfferMessage(request.getClientMessage() != null ? request.getClientMessage().trim() : null);

        if (!isCounterOffer(loan)) {
            throw new BusinessRuleException(
                    "Cette proposition correspond à l'offre système. Utilisez « Approuver le dossier » directement."
            );
        }

        loan.setOfferClientAccepted(false);
        loan.setStatus(LoanApplicationStatus.OFFER_PENDING);
        LoanApplication saved = loanRepository.save(loan);

        historyService.recordEvent(
                saved,
                LoanApplicationEventType.OFFER_PROPOSED,
                actorTypeFor(currentUser),
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(
                        "approvedAmount", saved.getApprovedAmount(),
                        "approvedDurationMonths", saved.getApprovedDurationMonths(),
                        "interestRate", saved.getInterestRate(),
                        "clientMessage", saved.getOfferMessage() != null ? saved.getOfferMessage() : ""
                )
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto acceptOffer(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!isApplicant(loan, currentUser)) {
            throw new ForbiddenOperationException("Seul le demandeur peut accepter l'offre.");
        }

        if (loan.getStatus() != LoanApplicationStatus.OFFER_PENDING) {
            throw new BusinessRuleException("Aucune contre-offre en attente de votre réponse.");
        }

        loan.setOfferClientAccepted(true);
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        LoanApplication saved = loanRepository.save(loan);

        historyService.recordEvent(
                saved,
                LoanApplicationEventType.OFFER_ACCEPTED,
                LoanEventActorType.CLIENT,
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of()
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto rejectOffer(Long id, RejectOfferRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!isApplicant(loan, currentUser)) {
            throw new ForbiddenOperationException("Seul le demandeur peut refuser l'offre.");
        }

        if (loan.getStatus() != LoanApplicationStatus.OFFER_PENDING) {
            throw new BusinessRuleException("Aucune contre-offre en attente de votre réponse.");
        }

        String comment = request != null && request.getComment() != null
                ? request.getComment().trim()
                : null;
        if (comment != null && comment.isBlank()) {
            comment = null;
        }

        loan.setOfferClientAccepted(false);
        loan.setApprovedAmount(null);
        loan.setApprovedDurationMonths(null);
        loan.setInterestRate(null);
        loan.setOfferMessage(null);
        loan.setStatus(LoanApplicationStatus.UNDER_REVIEW);
        LoanApplication saved = loanRepository.save(loan);

        Map<String, Object> payload = comment != null ? Map.of(PAYLOAD_COMMENT, comment) : Map.of();
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.OFFER_REJECTED,
                LoanEventActorType.CLIENT,
                currentUser.getEmail(),
                displayName(currentUser),
                payload
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto approveApplication(Long id, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
        }

        ensureAdvisorCanManage(loan, currentUser);

        if (loan.getStatus() == LoanApplicationStatus.OFFER_PENDING) {
            throw new BusinessRuleException(
                    "Approbation impossible : en attente de la réponse du client sur la contre-offre."
            );
        }

        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Approbation impossible : le dossier doit être en analyse (UNDER_REVIEW)."
            );
        }

        if (isCounterOffer(loan) && !Boolean.TRUE.equals(loan.getOfferClientAccepted())) {
            throw new BusinessRuleException(
                    "Approbation impossible : le client doit accepter la contre-offre avant approbation."
            );
        }

        ensureRequiredDocumentsPresent(loan.getId());
        documentReviewService.ensureRequiredDocumentsReadyForDecision(loan.getId(), "Approbation impossible");

        if (hasRole(currentUser, ROLE_CONSEILLER)) {
            assignAdvisorIfNeeded(loan, currentUser);
        }

        if (!isCounterOffer(loan)) {
            applySystemOffer(loan);
        }

        if (!isCompleteForApproval(loan)) {
            throw new BusinessRuleException(
                    "Approbation impossible : tous les champs requis du dossier ne sont pas remplis."
            );
        }

        loan.setStatus(LoanApplicationStatus.APPROVED);
        loan.setDecidedAt(Instant.now());

        LoanApplication saved = loanRepository.save(loan);
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.APPLICATION_APPROVED,
                actorTypeFor(currentUser),
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(PAYLOAD_COMMENT, saved.getDecisionComment() != null ? saved.getDecisionComment() : "")
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto rejectApplication(Long id, RejectLoanRequestDto request, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));

        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
        }

        ensureAdvisorCanManage(loan, currentUser);

        if (!(loan.getStatus() == LoanApplicationStatus.SUBMITTED
                || loan.getStatus() == LoanApplicationStatus.UNDER_REVIEW
                || loan.getStatus() == LoanApplicationStatus.OFFER_PENDING)) {
            throw new BusinessRuleException(
                    "Rejet impossible : le dossier doit être en cours d'instruction."
            );
        }

        String comment = request.getComment().trim();
        loan.setStatus(LoanApplicationStatus.REJECTED);
        loan.setDecidedAt(Instant.now());
        loan.setDecisionComment(comment);

        LoanApplication saved = loanRepository.save(loan);
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.APPLICATION_REJECTED,
                actorTypeFor(currentUser),
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(PAYLOAD_COMMENT, comment)
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanResponseDto cancelApplication(
            Long id,
            CancelLoanRequestDto request,
            String currentUserEmail
    ) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);

        if (!isApplicant(loan, currentUser)) {
            throw new ForbiddenOperationException("Seul le demandeur peut annuler sa demande.");
        }

        if (loan.getStatus() != LoanApplicationStatus.SUBMITTED
                && loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW
                && loan.getStatus() != LoanApplicationStatus.OFFER_PENDING) {
            throw new BusinessRuleException(
                    "Annulation impossible : seuls les dossiers soumis, en analyse ou en attente d'offre peuvent être annulés."
            );
        }

        String comment = request != null && request.getComment() != null
                ? request.getComment().trim()
                : "";
        if (comment.isBlank()) {
            comment = "Annulée à la demande du client.";
        }

        loan.setStatus(LoanApplicationStatus.CANCELLED);
        loan.setDecidedAt(Instant.now());
        loan.setDecisionComment(comment);

        LoanApplication saved = loanRepository.save(loan);
        historyService.recordEvent(
                saved,
                LoanApplicationEventType.APPLICATION_CANCELLED,
                LoanEventActorType.CLIENT,
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(PAYLOAD_COMMENT, comment)
        );
        return mapToResponseDto(saved);
    }

    @Transactional
    public LoanDocumentResponseDto uploadDocument(
            Long loanId,
            LoanDocumentType documentType,
            MultipartFile file,
            String displayName,
            String currentUserEmail
    ) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanEditDraft(loan);
        String resolvedDisplayName = resolveDisplayName(documentType, displayName, loanId);
        LoanDocumentStorageService.StoredUpload stored = documentStorage.storeUpload(loanId, file);

        LoanDocument document = LoanDocument.builder()
                .loanApplication(loan)
                .documentType(documentType)
                .originalFileName(stored.originalFileName())
                .displayName(resolvedDisplayName)
                .storedFileName(stored.storedFileName())
                .contentType(stored.contentType())
                .fileSizeBytes(stored.fileSizeBytes())
                .storagePath(stored.storagePath())
                .build();

        LoanDocument savedDoc = loanDocumentRepository.save(document);
        return mapToDocumentResponse(savedDoc);
    }

    @Transactional
    public LoanDocumentResponseDto uploadComplementDocument(
            Long loanId,
            LoanDocumentType documentType,
            MultipartFile file,
            String displayName,
            String currentUserEmail
    ) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanUploadComplement(loan);
        String resolvedDisplayName = resolveDisplayName(documentType, displayName, loanId);
        LoanDocumentStorageService.StoredUpload stored = documentStorage.storeUpload(loanId, file);

        LoanDocument document = LoanDocument.builder()
                .loanApplication(loan)
                .documentType(documentType)
                .originalFileName(stored.originalFileName())
                .displayName(resolvedDisplayName)
                .storedFileName(stored.storedFileName())
                .contentType(stored.contentType())
                .fileSizeBytes(stored.fileSizeBytes())
                .storagePath(stored.storagePath())
                .build();

        LoanDocument savedDoc = loanDocumentRepository.save(document);
        documentReviewService.markPendingReview(loan, documentType);
        historyService.recordEvent(
                loan,
                LoanApplicationEventType.DOCUMENT_UPLOADED,
                LoanEventActorType.CLIENT,
                currentUser.getEmail(),
                displayName(currentUser),
                Map.of(
                        PAYLOAD_DOCUMENT_TYPE, documentType.name(),
                        "fileName", savedDoc.getOriginalFileName(),
                        "complement", true
                )
        );
        return mapToDocumentResponse(savedDoc);
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentReviewResponseDto> getDocumentReviews(Long loanId, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        return documentReviewService.getDocumentReviews(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDto> getDocuments(Long loanId, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        return loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId)
                .stream()
                .map(this::mapToDocumentResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(Long loanId, Long documentId, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureCanEditDraft(loan);

        LoanDocument document = loanDocumentRepository.findByIdAndLoanApplicationId(documentId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));

        documentStorage.deleteFile(document.getStoragePath());
        loanDocumentRepository.delete(document);
        Set<String> referenced = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId)
                .stream()
                .map(LoanDocument::getStoredFileName)
                .collect(java.util.stream.Collectors.toSet());
        documentStorage.cleanupLoanDirectoryOrphans(loanId, referenced);
    }

    @Transactional(readOnly = true)
    public LoanDocumentStorageService.DownloadedFile downloadDocument(Long loanId, Long documentId, String currentUserEmail) {
        User currentUser = getRequiredUser(currentUserEmail);
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DOSSIER_INTROUVABLE));
        ensureCanAccessLoan(loan, currentUser);
        ensureAdvisorCanAccessDocuments(loan, currentUser);

        LoanDocument document = loanDocumentRepository.findByIdAndLoanApplicationId(documentId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));

        return documentStorage.readFile(
                document.getStoragePath(),
                document.getOriginalFileName(),
                document.getContentType()
        );
    }

    private String generateUniqueReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!loanRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new BusinessRuleException("Impossible de générer une référence unique de dossier.");
    }

    private User getRequiredUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private boolean isApplicant(LoanApplication loan, User user) {
        return loan.getApplicant() != null && loan.getApplicant().getId().equals(user.getId());
    }

    private boolean isAssignedAdvisor(LoanApplication loan, User user) {
        return loan.getAssignedAdvisor() != null && loan.getAssignedAdvisor().getId().equals(user.getId());
    }

    private void ensureCanAccessLoan(LoanApplication loan, User currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }
        if (hasRole(currentUser, ROLE_CONSEILLER)) {
            if (isAssignedAdvisor(loan, currentUser)) {
                return;
            }
            if (loan.getStatus() == LoanApplicationStatus.SUBMITTED && loan.getAssignedAdvisor() == null) {
                return;
            }
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
        }
        if (isApplicant(loan, currentUser)) {
            return;
        }
        throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
    }

    private void ensureAdvisorCanManage(LoanApplication loan, User currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }
        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
        }
        if (isAssignedAdvisor(loan, currentUser)) {
            return;
        }
        if (loan.getStatus() == LoanApplicationStatus.SUBMITTED && loan.getAssignedAdvisor() == null) {
            return;
        }
        throw new ForbiddenOperationException(MSG_ACCES_REFUSE_DEMANDE);
    }

    private void ensureAdvisorCanAccessDocuments(LoanApplication loan, User currentUser) {
        if (isApplicant(loan, currentUser)) {
            return;
        }
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }
        if (!hasRole(currentUser, ROLE_CONSEILLER)) {
            return;
        }
        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Consultation des pièces impossible : le dossier doit être en analyse (UNDER_REVIEW)."
            );
        }
    }

    private boolean assignAdvisorIfNeeded(LoanApplication loan, User advisor) {
        if (loan.getAssignedAdvisor() != null) {
            return false;
        }
        loan.setAssignedAdvisor(advisor);
        return true;
    }

    private void applySystemOffer(LoanApplication loan) {
        loan.setApprovedAmount(loan.getRequestedAmount());
        loan.setApprovedDurationMonths(loan.getRequestedDurationMonths());
        loan.setInterestRate(SYSTEM_INTEREST_RATE);
        loan.setOfferMessage(null);
        loan.setOfferClientAccepted(null);
    }

    private boolean isCounterOffer(LoanApplication loan) {
        if (loan.getApprovedAmount() == null
                || loan.getApprovedDurationMonths() == null
                || loan.getInterestRate() == null) {
            return false;
        }
        return loan.getApprovedAmount().compareTo(loan.getRequestedAmount()) != 0
                || !loan.getApprovedDurationMonths().equals(loan.getRequestedDurationMonths())
                || loan.getInterestRate().compareTo(SYSTEM_INTEREST_RATE) != 0;
    }

    private void ensureCanSubmit(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Soumission impossible : seul un dossier en brouillon (DRAFT) peut être soumis."
            );
        }
    }

    private void ensureCanUploadComplement(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Dépôt impossible : complément autorisé uniquement pour un dossier en analyse (UNDER_REVIEW)."
            );
        }
    }

    private LoanEventActorType actorTypeFor(User user) {
        if (hasRole(user, ROLE_ADMIN)) {
            return LoanEventActorType.ADMIN;
        }
        if (hasRole(user, ROLE_CONSEILLER)) {
            return LoanEventActorType.ADVISOR;
        }
        return LoanEventActorType.CLIENT;
    }

    private String displayName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private void ensureAdmin(User user) {
        if (!hasRole(user, ROLE_ADMIN)) {
            throw new ForbiddenOperationException("Accès réservé aux administrateurs.");
        }
    }

    private Sort adminListSort(AdminLoanListSort sort) {
        AdminLoanListSort resolved = sort == null ? AdminLoanListSort.UPDATED_DESC : sort;
        return switch (resolved) {
            case UPDATED_ASC -> Sort.by(Sort.Direction.ASC, "updatedAt");
            case AMOUNT_DESC -> Sort.by(Sort.Direction.DESC, "requestedAmount");
            case AMOUNT_ASC -> Sort.by(Sort.Direction.ASC, "requestedAmount");
            case UPDATED_DESC -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }

    private void recordInitialDocumentsBundle(LoanApplication loan, User client, Instant submittedAt) {
        List<LoanDocument> documents = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loan.getId());
        if (documents.isEmpty()) {
            return;
        }
        long requiredTypesPresent = documents.stream()
                .map(LoanDocument::getDocumentType)
                .filter(REQUIRED_DOCUMENT_TYPES::contains)
                .distinct()
                .count();
        List<String> documentTypes = documents.stream()
                .map(d -> d.getDocumentType().name())
                .distinct()
                .toList();
        Instant occurredAt = submittedAt != null
                ? submittedAt.minus(1, ChronoUnit.SECONDS)
                : Instant.now();
        historyService.recordEvent(
                loan,
                LoanApplicationEventType.DOCUMENT_UPLOADED,
                LoanEventActorType.CLIENT,
                client.getEmail(),
                displayName(client),
                Map.of(
                        "bundled", true,
                        "documentCount", documents.size(),
                        "requiredDocumentCount", requiredTypesPresent,
                        "documentTypes", documentTypes
                ),
                occurredAt
        );
    }

    private void ensureRequiredDocumentsPresent(Long loanId) {
        List<LoanDocument> documents = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId);
        Set<LoanDocumentType> presentTypes = documents.stream()
                .map(LoanDocument::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        for (LoanDocumentType required : REQUIRED_DOCUMENT_TYPES) {
            if (!presentTypes.contains(required)) {
                throw new BusinessRuleException(
                        "Soumission impossible : le document obligatoire « " + required + " » est manquant."
                );
            }
        }
    }

    private void ensureDocumentFilePresent(Long loanId, LoanDocumentType documentType) {
        if (documentType == null) {
            throw new BusinessRuleException("Validation impossible : le type de document est obligatoire.");
        }
        boolean present = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(loanId).stream()
                .anyMatch(document -> document.getDocumentType() == documentType);
        if (!present) {
            throw new BusinessRuleException(
                    "Validation impossible : aucune pièce « " + documentType + " » n'a été déposée pour ce dossier."
            );
        }
    }

    private void applyRequestToLoan(LoanApplication loan, LoanRequestDto request) {
        loan.setTitle(request.getTitle());
        loan.setLoanPurpose(request.getLoanPurpose());
        loan.setPurpose(resolvePurposeLabel(request.getLoanPurpose()));
        loan.setRequestedAmount(request.getRequestedAmount());
        loan.setRequestedDurationMonths(request.getRequestedDurationMonths());
        loan.setComment(request.getComment());
        loan.setMonthlyIncome(request.getMonthlyIncome());
        loan.setEmploymentStatus(request.getEmploymentStatus());
        loan.setAdditionalIncome(defaultZero(request.getAdditionalIncome()));
        loan.setEmployerName(request.getEmployerName());
        loan.setJobTitle(blankToNull(request.getJobTitle()));
        loan.setEmployerSector(blankToNull(request.getEmployerSector()));
        loan.setHireDate(request.getHireDate());
        loan.setSeniorityMonths(request.getSeniorityMonths());
        loan.setMonthlyRent(defaultZero(request.getMonthlyRent()));
        loan.setMonthlyLoanPayments(defaultZero(request.getMonthlyLoanPayments()));
        loan.setMonthlyAlimony(defaultZero(request.getMonthlyAlimony()));
        loan.setMonthlyOtherCharges(defaultZero(request.getMonthlyOtherCharges()));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolvePurposeLabel(LoanPurpose loanPurpose) {
        return switch (loanPurpose) {
            case SOFTWARE -> "Logiciel / équipement professionnel";
            case VEHICLE -> "Véhicule";
            case HOME_IMPROVEMENT -> "Travaux / aménagement";
            case PERSONAL -> "Projet personnel";
            case EDUCATION -> "Formation / études";
            case OTHER -> "Autre";
        };
    }

    private void ensureCanEditDraft(LoanApplication loan) {
        if (loan.getStatus() != LoanApplicationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Modification impossible : seul un dossier en brouillon (DRAFT) peut être modifié."
            );
        }
    }

    private boolean isCompleteForApproval(LoanApplication loan) {
        return loan.getRequestedAmount() != null
                && loan.getRequestedDurationMonths() != null
                && loan.getPurpose() != null && !loan.getPurpose().isBlank()
                && loan.getMonthlyIncome() != null
                && loan.getEmploymentStatus() != null
                && loan.getAssignedAdvisor() != null
                && loan.getApprovedAmount() != null
                && loan.getApprovedDurationMonths() != null
                && loan.getInterestRate() != null;
    }

    private LoanResponseDto mapToResponseDto(LoanApplication loan) {
        return LoanResponseDto.builder()
                .id(loan.getId())
                .reference(loan.getReference())
                .status(loan.getStatus())
                .title(loan.getTitle())
                .loanPurpose(loan.getLoanPurpose())
                .requestedAmount(loan.getRequestedAmount())
                .requestedDurationMonths(loan.getRequestedDurationMonths())
                .purpose(loan.getPurpose())
                .comment(loan.getComment())
                .monthlyIncome(loan.getMonthlyIncome())
                .employmentStatus(loan.getEmploymentStatus())
                .additionalIncome(loan.getAdditionalIncome())
                .employerName(loan.getEmployerName())
                .jobTitle(loan.getJobTitle())
                .employerSector(loan.getEmployerSector())
                .hireDate(loan.getHireDate())
                .seniorityMonths(loan.getSeniorityMonths())
                .monthlyRent(loan.getMonthlyRent())
                .monthlyLoanPayments(loan.getMonthlyLoanPayments())
                .monthlyAlimony(loan.getMonthlyAlimony())
                .monthlyOtherCharges(loan.getMonthlyOtherCharges())
                .submittedAt(loan.getSubmittedAt())
                .decidedAt(loan.getDecidedAt())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .decisionComment(loan.getDecisionComment())
                .approvedAmount(loan.getApprovedAmount())
                .approvedDurationMonths(loan.getApprovedDurationMonths())
                .interestRate(loan.getInterestRate())
                .offerMessage(loan.getOfferMessage())
                .offerClientAccepted(loan.getOfferClientAccepted())
                .applicantId(loan.getApplicant() != null ? loan.getApplicant().getId() : null)
                .applicantName(
                        loan.getApplicant() != null
                                ? (loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName()).trim()
                                : null
                )
                .applicantEmail(loan.getApplicant() != null ? loan.getApplicant().getEmail() : null)
                .advisorId(loan.getAssignedAdvisor() != null ? loan.getAssignedAdvisor().getId() : null)
                .advisorName(
                        loan.getAssignedAdvisor() != null
                                ? (loan.getAssignedAdvisor().getFirstName() + " " + loan.getAssignedAdvisor().getLastName()).trim()
                                : null
                )
                .build();
    }

    private LoanDocumentResponseDto mapToDocumentResponse(LoanDocument document) {
        return LoanDocumentResponseDto.builder()
                .id(document.getId())
                .loanApplicationId(document.getLoanApplication().getId())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .displayName(document.getDisplayName())
                .contentType(document.getContentType())
                .fileSizeBytes(document.getFileSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private String resolveDisplayName(LoanDocumentType documentType, String displayName, Long loanId) {
        if (documentType == LoanDocumentType.OTHER) {
            long otherCount = loanDocumentRepository.countByLoanApplicationIdAndDocumentType(
                    loanId, LoanDocumentType.OTHER);
            if (otherCount >= MAX_OTHER_DOCUMENTS) {
                throw new BusinessRuleException(
                        "Maximum " + MAX_OTHER_DOCUMENTS + " documents pour la catégorie « Autre document »."
                );
            }
            if (displayName == null || displayName.isBlank()) {
                throw new BusinessRuleException(
                        "Le nom du document est obligatoire pour la catégorie « Autre document »."
                );
            }
            String trimmed = displayName.trim();
            if (trimmed.length() > MAX_DOCUMENT_DISPLAY_NAME_LENGTH) {
                throw new BusinessRuleException(
                        "Le nom du document ne doit pas dépasser " + MAX_DOCUMENT_DISPLAY_NAME_LENGTH + " caractères."
                );
            }
            return trimmed;
        }
        return null;
    }

}
