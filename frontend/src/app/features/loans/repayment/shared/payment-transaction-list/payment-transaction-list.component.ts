import { CurrencyPipe, DatePipe, UpperCasePipe } from '@angular/common';

import { Component, computed, effect, input, signal } from '@angular/core';

import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { MatTableModule } from '@angular/material/table';

import { PaymentTransactionDto } from '../../../../../core/loans/repayment/models/payment-transaction.model';



const PAGE_SIZE = 8;



export type PaymentTransactionListVariant = 'table' | 'list';



@Component({

  selector: 'app-payment-transaction-list',

  standalone: true,

  imports: [MatTableModule, MatPaginatorModule, CurrencyPipe, DatePipe, UpperCasePipe],

  templateUrl: './payment-transaction-list.component.html',

  styleUrl: './payment-transaction-list.component.scss',

})

export class PaymentTransactionListComponent {

  readonly PAGE_SIZE = PAGE_SIZE;

  readonly transactions = input.required<PaymentTransactionDto[]>();

  readonly variant = input<PaymentTransactionListVariant>('table');

  readonly displayedColumns = [

    'attemptedAt',

    'sequenceNumber',

    'amount',

    'status',

    'failureReason',

  ];



  readonly pageIndex = signal(0);



  readonly pageTransactions = computed(() => {

    const start = this.pageIndex() * PAGE_SIZE;

    return this.transactions().slice(start, start + PAGE_SIZE);

  });



  readonly rangeStart = computed(() => {

    const total = this.transactions().length;

    if (!total) {

      return 0;

    }

    return this.pageIndex() * PAGE_SIZE + 1;

  });



  readonly rangeEnd = computed(() => {

    const total = this.transactions().length;

    if (!total) {

      return 0;

    }

    return Math.min((this.pageIndex() + 1) * PAGE_SIZE, total);

  });



  constructor() {

    effect(() => {

      this.transactions();

      this.pageIndex.set(0);

    });

  }



  onPage(event: PageEvent): void {

    this.pageIndex.set(event.pageIndex);

  }



  statusLabel(status: string): string {

    if (status === 'SUCCESS') {

      return 'Succès';

    }

    if (status === 'FAILED') {

      return 'Échec';

    }

    return status;

  }

}


