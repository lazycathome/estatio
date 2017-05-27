package org.estatio.capex.dom.documents.invoice;

import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.estatio.capex.dom.order.OrderRepository;
import org.estatio.dom.financial.bankaccount.BankAccount;
import org.estatio.dom.financial.bankaccount.BankAccountRepository;
import org.estatio.dom.party.Organisation;
import org.estatio.dom.party.Party;

import static org.assertj.core.api.Assertions.assertThat;

public class IncomingInvoiceViewModel_autoCompleteBankAccount_Test {

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

    @Mock
    private OrderRepository mockOrderRepository;

    @Mock
    private BankAccountRepository mockBankAccountRepository;

    @Test
    public void autoCompleteBankAccount_works() throws Exception {

        List<BankAccount> result;

        // given
        IncomingInvoiceViewModel vm = new IncomingInvoiceViewModel() {
            IncomingInvoiceViewModel setBankAccountRepository(BankAccountRepository bankAccountRepository) {
                this.bankAccountRepository = bankAccountRepository;
                return this;
            }
        }.setBankAccountRepository(mockBankAccountRepository);

        BankAccount acc1 = new BankAccount();
        acc1.setReference("123");
        BankAccount acc2 = new BankAccount();
        acc2.setReference("345");

        Party owner = new Organisation();
        acc2.setOwner(owner);

        // expect
        context.checking(new Expectations() {
            {
                allowing(mockBankAccountRepository).allBankAccounts();
                will(returnValue(Arrays.asList(
                        acc1, acc2
                )));
                oneOf(mockBankAccountRepository).findBankAccountsByOwner(owner);
                will(returnValue(Arrays.asList(
                        acc2
                )));
            }

        });

        // when
        result = vm.autoCompleteBankAccount("23");

        // then
        assertThat(result.size()).isEqualTo(1);

        // and when
        result = vm.autoCompleteBankAccount("3");

        // then
        assertThat(result.size()).isEqualTo(2);

        // and when seller is set
        vm.setSeller(owner);
        result = vm.autoCompleteBankAccount("3");

        // then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(acc2);

    }
}