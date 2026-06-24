package com.projetfilrouge.loanmanagement.service.documents;

/**
 * Fichier téléchargeable renvoyé par les services documents.
 */
public record DocumentDownload(String fileName, String contentType, byte[] content) {
}
