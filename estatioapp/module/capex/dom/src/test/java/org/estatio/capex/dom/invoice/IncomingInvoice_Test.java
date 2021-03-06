package org.estatio.capex.dom.invoice;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.joda.time.LocalDate;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.estatio.capex.dom.project.Project;
import org.estatio.dom.asset.Property;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.financial.bankaccount.BankAccount;
import org.estatio.dom.invoice.PaymentMethod;
import org.estatio.dom.party.Organisation;
import org.estatio.dom.tax.Tax;

import static org.assertj.core.api.Assertions.assertThat;

public class IncomingInvoice_Test {

    IncomingInvoice incomingInvoice;

    @Test
    public void recalculateAmounts() throws Exception {

        // given
        incomingInvoice = new IncomingInvoice();
        IncomingInvoiceItem item1 = new IncomingInvoiceItem();
        IncomingInvoiceItem item2 = new IncomingInvoiceItem();
        IncomingInvoiceItem item3 = new IncomingInvoiceItem();
        IncomingInvoiceItem item4 = new IncomingInvoiceItem();
        item1.setNetAmount(new BigDecimal("100.00"));
        item1.setGrossAmount(new BigDecimal("120.00"));
        item2.setNetAmount(new BigDecimal("50.00"));
        item2.setGrossAmount(new BigDecimal("55.00"));
        item3.setNetAmount(null); // explicit for test
        item3.setGrossAmount(new BigDecimal("-1.00"));
        item4.setNetAmount(new BigDecimal("-1.00"));
        item4.setGrossAmount(null); // explicit for test
        incomingInvoice.getItems().addAll(Arrays.asList(item1, item2, item3, item4));

        // when
        incomingInvoice.recalculateAmounts();

        // then
        Assertions.assertThat(incomingInvoice.getNetAmount()).isEqualTo(new BigDecimal("149.00"));
        Assertions.assertThat(incomingInvoice.getGrossAmount()).isEqualTo(new BigDecimal("174.00"));

    }

    @Test
    public void minimalRequiredDataToComplete() throws Exception {

        // given
        IncomingInvoice invoice = new IncomingInvoice();
        invoice.setPaymentMethod(PaymentMethod.MANUAL_PROCESS);
        invoice.setBankAccount(new BankAccount());

        IncomingInvoiceItem item1 = new IncomingInvoiceItem();
        item1.setSequence(BigInteger.ONE);
        invoice.getItems().add(item1);

        // when neither conditions on invoice and items satisfied
        String result = invoice.reasonIncomplete();
        // then
        assertThat(result).isEqualTo("incoming invoice type, invoice number, buyer, seller, date received, due date, net amount, gross amount, (on item 1) incoming invoice type, start date, end date, net amount, vat amount, gross amount, charge required");

        // and when conditions on item satisfied
        invoice.setType(IncomingInvoiceType.PROPERTY_EXPENSES);
        invoice.setInvoiceNumber("123");
        invoice.setNetAmount(new BigDecimal("100"));
        item1.setIncomingInvoiceType(IncomingInvoiceType.LOCAL_EXPENSES);
        item1.setStartDate(new LocalDate());
        item1.setEndDate(new LocalDate());
        item1.setNetAmount(new BigDecimal("100"));
        item1.setGrossAmount(new BigDecimal("100"));
        item1.setVatAmount(BigDecimal.ZERO);
        item1.setCharge(new Charge());
        result = invoice.reasonIncomplete();
        // then
        assertThat(result).isEqualTo("buyer, seller, date received, due date, gross amount, property required");

        // and when conditions for invoice satisfied
        item1.setIncomingInvoiceType(IncomingInvoiceType.CAPEX);
        invoice.setBuyer(new Organisation());
        invoice.setSeller(new Organisation());
        invoice.setBankAccount(new BankAccount());
        invoice.setDateReceived(new LocalDate());
        invoice.setDueDate(new LocalDate());
        invoice.setGrossAmount(BigDecimal.ZERO);
        invoice.setProperty(new Property());
        result = invoice.reasonIncomplete();
        // then
        assertThat(result).isEqualTo("(on item 1) project (capex), fixed asset required");

        // and when all conditions satisfied
        item1.setFixedAsset(new Property());
        item1.setProject(new Project());
        result = invoice.reasonIncomplete();
        // then
        assertThat(result).isNull();

    }

    @Test
    public void validator_checkNotNull_works() throws Exception {

        String result;

        // given
        IncomingInvoice.Validator validator = new IncomingInvoice.Validator();

        // when condition satisfied
        result = validator.checkNotNull(new Object(), "some property name").getResult();
        // then
        Assertions.assertThat(result).isNull();

        // and when not conditions satisfied
        result = validator.checkNotNull(null, "some property name").getResult();
        // then
        Assertions.assertThat(result).isEqualTo("some property name required");

    }

    @Test
    public void validator_validateForIncomingInvoiceType_works() throws Exception {

        String result;
        IncomingInvoice.Validator validator;
        IncomingInvoice invoice;

        // given
        validator = new IncomingInvoice.Validator();
        invoice = new IncomingInvoice();
        // when
        invoice.setType(IncomingInvoiceType.CAPEX);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isEqualTo("property required");

        // given
        validator = new IncomingInvoice.Validator();
        invoice = new IncomingInvoice();
        // when
        invoice.setType(IncomingInvoiceType.PROPERTY_EXPENSES);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isEqualTo("property required");

        // given
        validator = new IncomingInvoice.Validator();
        invoice = new IncomingInvoice();
        // when
        invoice.setType(IncomingInvoiceType.SERVICE_CHARGES);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isEqualTo("property required");

        // given
        validator = new IncomingInvoice.Validator();
        invoice = new IncomingInvoice();
        // and when all conditions satisfied
        invoice.setType(IncomingInvoiceType.LOCAL_EXPENSES);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isNull();

        // and when all conditions satisfied
        invoice.setType(IncomingInvoiceType.INTERCOMPANY);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isNull();

        // and when all conditions satisfied
        invoice.setType(IncomingInvoiceType.TANGIBLE_FIXED_ASSET);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isNull();

        // and when all conditions satisfied
        invoice.setType(IncomingInvoiceType.RE_INVOICING);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isNull();

        // and when all conditions satisfied
        invoice.setType(IncomingInvoiceType.CORPORATE_EXPENSES);
        result = validator.validateForIncomingInvoiceType(invoice).getResult();
        // then
        Assertions.assertThat(result).isNull();

    }

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

    @Mock
    IncomingInvoiceItemRepository mockIncomingInvoiceItemRepository;

    @Test
    public void splitItem_mixin_works() throws Exception {

        // given
        IncomingInvoice invoice = new IncomingInvoice();
        invoice.setType(IncomingInvoiceType.CAPEX);
        IncomingInvoice.splitItem mixin = new IncomingInvoice.splitItem(invoice);
        mixin.incomingInvoiceItemRepository = mockIncomingInvoiceItemRepository;

        LocalDate dueDate = new LocalDate(2018,01,01);
        invoice.setDueDate(dueDate);

        String description = "some description";
        Tax tax = new Tax();
        Charge charge = new Charge();
        Property property = new Property();
        Project project = new Project();
        BudgetItem budgetItem = new BudgetItem();
        String period = "F2018";

        IncomingInvoiceItem itemToSplit = new IncomingInvoiceItem();
        itemToSplit.setNetAmount(new BigDecimal("200.00"));
        itemToSplit.setVatAmount(new BigDecimal("40.00"));
        itemToSplit.setGrossAmount(new BigDecimal("240.00"));

        BigDecimal newItemNetAmount = new BigDecimal("50.00");
        BigDecimal newItemVatAmount = new BigDecimal("10");
        BigDecimal newItemGrossAmount = new BigDecimal("60.00");

        // expect
        context.checking(new Expectations(){{
            oneOf(mockIncomingInvoiceItemRepository).addItem(
                    invoice,
                    IncomingInvoiceType.CAPEX,
                    charge,
                    description,
                    newItemNetAmount,
                    newItemVatAmount,
                    newItemGrossAmount,
                    tax,
                    dueDate,
                    period,
                    property,
                    project,
                    budgetItem);
        }});

        // when
        mixin.act(itemToSplit, description, newItemNetAmount, newItemVatAmount, tax, newItemGrossAmount,charge, property, project, budgetItem, period);

        // then
        Assertions.assertThat(itemToSplit.getNetAmount()).isEqualTo(new BigDecimal("150.00"));
        Assertions.assertThat(itemToSplit.getVatAmount()).isEqualTo(new BigDecimal("30.00"));
        Assertions.assertThat(itemToSplit.getGrossAmount()).isEqualTo(new BigDecimal("180.00"));

    }

    @Test
    public void mergeItem_mixin_works() throws Exception {

        // given
        IncomingInvoice invoice = new IncomingInvoice();
        IncomingInvoice.mergeItems mixin = new IncomingInvoice.mergeItems(invoice);
        mixin.incomingInvoiceItemRepository = mockIncomingInvoiceItemRepository;

        IncomingInvoiceItem sourceItem = new IncomingInvoiceItem();
        IncomingInvoiceItem targetItem = new IncomingInvoiceItem();

        // expect
        context.checking(new Expectations(){{
            oneOf(mockIncomingInvoiceItemRepository).mergeItems(
                    sourceItem, targetItem);
        }});

        // when
        mixin.act(sourceItem, targetItem);

    }

    @Ignore // WIP
    @Test
    public void reasonDisabledDueToState() {
        IncomingInvoice invoice = new IncomingInvoice();

        invoice.reasonDisabledDueToState(invoice);

    }
}