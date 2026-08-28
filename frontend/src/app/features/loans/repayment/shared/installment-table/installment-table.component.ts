import { CurrencyPipe, DatePipe, UpperCasePipe } from '@angular/common';

import { Component, computed, effect, input, signal } from '@angular/core';

import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { MatTableModule } from '@angular/material/table';

import { InstallmentDto } from '../../../../../core/loans/repayment/models/installment.model';

import { installmentStatusStyle } from '../../../../../core/loans/repayment/constants/repayment.constants';



const PAGE_SIZE = 8;



export type InstallmentTableVariant = 'default' | 'schedule';



@Component({

  selector: 'app-installment-table',

  standalone: true,

  imports: [MatTableModule, MatPaginatorModule, CurrencyPipe, DatePipe, UpperCasePipe],

  templateUrl: './installment-table.component.html',

  styleUrl: './installment-table.component.scss',

})

export class InstallmentTableComponent {

  readonly PAGE_SIZE = PAGE_SIZE;

  readonly installments = input.required<InstallmentDto[]>();

  readonly variant = input<InstallmentTableVariant>('default');



  readonly displayedColumns = computed(() =>

    this.variant() === 'schedule'

      ? [

          'sequenceNumber',

          'dueDate',

          'amountDue',

          'principalPart',

          'interestPart',

          'remainingBalance',

          'status',

        ]

      : ['sequenceNumber', 'dueDate', 'amountDue', 'breakdown', 'status'],

  );



  readonly pageIndex = signal(0);



  readonly pageInstallments = computed(() => {

    const start = this.pageIndex() * PAGE_SIZE;

    return this.installments().slice(start, start + PAGE_SIZE);

  });



  readonly rangeStart = computed(() => {

    const total = this.installments().length;

    if (!total) {

      return 0;

    }

    return this.pageIndex() * PAGE_SIZE + 1;

  });



  readonly rangeEnd = computed(() => {

    const total = this.installments().length;

    if (!total) {

      return 0;

    }

    return Math.min((this.pageIndex() + 1) * PAGE_SIZE, total);

  });



  constructor() {

    effect(() => {

      this.installments();

      this.pageIndex.set(0);

    });

  }



  onPage(event: PageEvent): void {

    this.pageIndex.set(event.pageIndex);

  }



  statusStyle(status: string) {

    return installmentStatusStyle(status);

  }

}


