package org.estatio.capex.dom.order.recategorize;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.eventbus.Subscribe;

import org.apache.isis.applib.AbstractSubscriber;
import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.repository.RepositoryService;

import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;
import org.incode.module.document.dom.impl.types.DocumentTypeRepository;

import org.estatio.capex.dom.documents.LookupAttachedPdfService;
import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition;
import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransitionType;
import org.estatio.capex.dom.order.Order;
import org.estatio.capex.dom.order.OrderRepository;
import org.estatio.capex.dom.order.approval.OrderApprovalState;
import org.estatio.capex.dom.order.approval.OrderApprovalStateTransition;
import org.estatio.capex.dom.state.StateTransitionService;
import org.estatio.capex.dom.task.Task;
import org.estatio.dom.invoice.DocumentTypeData;

@Mixin(method = "act")
public class Order_recategorize {

    private final Order order;

    public Order_recategorize(Order order) {
        this.order = order;
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout(cssClassFa = "mail-reply", cssClass = "btn-danger")
    public Document act(@Nullable final String comment) {
        Document document = lookupPdf();
        document.setType(DocumentTypeData.INCOMING.findUsing(documentTypeRepository));
        
        stateTransitionService.trigger(
                document,
                IncomingDocumentCategorisationStateTransition.class,
                IncomingDocumentCategorisationStateTransitionType.RESET,
                comment,
                null);

        // use events to cascade delete, eg paperclips and state transitions/tasks
        orderRepository.delete(order);

        return document;
    }

    public String disableAct() {
        final Document documentIfAny = lookupPdf();
        if(documentIfAny == null) {
            return "Cannot locate document";
        }
        if(order.getApprovalState() != OrderApprovalState.NEW) {
            return "Only NEW orders can be recategorized";
        }
        return null;
    }

    private Document lookupPdf() {
        final Optional<Document> document = lookupAttachedPdfService.lookupOrderPdfFrom(order);
        return document.orElse(null);
    }

    @Inject
    DocumentTypeRepository documentTypeRepository;

    @Inject
    LookupAttachedPdfService lookupAttachedPdfService;

    @Inject
    OrderRepository orderRepository;

    @Inject
    StateTransitionService stateTransitionService;

    /////////////////////////////////////////////////////////////////


    @DomainService(nature = NatureOfService.DOMAIN)
    public static class OrderDeletionCascadePaperclipsSubscriber extends AbstractSubscriber {

        @Subscribe
        @org.axonframework.eventhandling.annotation.EventHandler
        public void on(Order.ObjectRemovingEvent event) {
            final Order incomingInvoice = event.getSource();
            paperclipRepository.deleteIfAttachedTo(incomingInvoice, PaperclipRepository.Policy.PAPERCLIPS_ONLY);
        }

        @Inject
        private PaperclipRepository paperclipRepository;

    }

    @DomainService(nature = NatureOfService.DOMAIN)
    public static class OrderDeletionCascadeStateTransitionsAndTasksSubscriber extends AbstractSubscriber {

        @Subscribe
        @org.axonframework.eventhandling.annotation.EventHandler
        public void on(Order.ObjectRemovingEvent event) {
            final Order order = event.getSource();
            final List<OrderApprovalStateTransition> transitions =
                    repository.findByDomainObject(order);
            final List<Task> tasksToDelete =
                    transitions.stream()
                        .map(OrderApprovalStateTransition::getTask)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            for (OrderApprovalStateTransition transition : transitions) {
                repositoryService.removeAndFlush(transition);
            }
            for (Task task : tasksToDelete) {
                repositoryService.removeAndFlush(task);
            }
        }

        @Inject
        private OrderApprovalStateTransition.Repository repository;
        @Inject
        private RepositoryService repositoryService;

    }


}
