package org.estatio.capex.dom.invoice.approval.triggers;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.services.title.TitleService;

import org.estatio.capex.dom.invoice.IncomingInvoice;
import org.estatio.capex.dom.invoice.approval.IncomingInvoiceApprovalStateTransitionType;
import org.estatio.capex.dom.payment.PaymentBatch;
import org.estatio.capex.dom.payment.PaymentLine;
import org.estatio.capex.dom.payment.PaymentLineRepository;
import org.estatio.capex.dom.payment.approval.PaymentBatchApprovalState;
import org.estatio.dom.party.Person;

@Mixin(method = "act")
public class IncomingInvoice_reject extends IncomingInvoice_triggerAbstract {

    private final IncomingInvoice incomingInvoice;

    public IncomingInvoice_reject(IncomingInvoice incomingInvoice) {
        super(incomingInvoice, IncomingInvoiceApprovalStateTransitionType.REJECT);
        this.incomingInvoice = incomingInvoice;
    }

    @Action()
    @ActionLayout(cssClassFa = "fa-thumbs-o-down", cssClass = "btn-warning")
    public Object act(
            final String role,
            @Nullable final Person personToAssignNextTo,
            final String reason) {

        final List<PaymentLine> paymentLines =
                paymentLineRepository.findByInvoice(incomingInvoice);
        // because of the disableXxx guard, this should return either 0 or 1 lines.
        for (PaymentLine paymentLine : paymentLines) {
            final PaymentBatch paymentBatch = paymentLine.getBatch();
            paymentBatch.removeLineFor(incomingInvoice);
        }

        trigger(personToAssignNextTo, null, reason);
        return objectToReturn();
    }

    protected Object objectToReturn() {
        return getDomainObject();
    }

    public boolean hideAct() {
        return cannotTransition();
    }

    public String disableAct() {

        final List<PaymentLine> paymentLines =
                paymentLineRepository.findByInvoice(incomingInvoice);
        for (PaymentLine paymentLine : paymentLines) {
            final PaymentBatch paymentBatch = paymentLine.getBatch();
            final PaymentBatchApprovalState state = paymentBatch.getApprovalState();
            if(state != PaymentBatchApprovalState.NEW && state != PaymentBatchApprovalState.DISCARDED) {
                return String.format("Invoice is in batch %s", titleService.titleOf(paymentBatch));
            }
        }

        return reasonGuardNotSatisified();
    }

    public String default0Act() {
        return enumPartyRoleTypeName();
    }

    public Person default1Act() {
        return defaultPersonToAssignNextTo();
    }

    public List<Person> choices1Act() {
        return choicesPersonToAssignNextTo();
    }

    @Inject
    PaymentLineRepository paymentLineRepository;

    @Inject
    TitleService titleService;

}
