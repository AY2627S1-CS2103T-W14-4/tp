package gigabyte.ui;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Locale;

import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.PaymentObligationOrdering;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/** Panel displaying payment obligations for the selected gig. */
public class PaymentObligationListPanel extends UiPart<Region> {
    private static final String FXML = "PaymentObligationListPanel.fxml";
    private static final String NO_GIG = "Select a gig to view its payment obligations.";
    private static final String NO_PAYMENTS = "This gig has no payment obligations.";
    private static final String PAID = "PAID";
    private static final String OUTSTANDING = "OUTSTANDING";
    private static final String OVERDUE = "OVERDUE";

    private final ObservableList<PaymentObligation> paymentObligations;
    private final FilteredList<PaymentObligation> filteredPayments;
    private final SortedList<PaymentObligation> sortedPayments;
    private final Clock clock;
    private final PauseTransition paymentStateRefreshTimer = new PauseTransition();
    private Gig selectedGig;

    @FXML
    private Label emptyState;

    @FXML
    private ListView<PaymentObligation> paymentListView;

    /** Creates a payment-obligation panel backed by {@code paymentObligations}. */
    public PaymentObligationListPanel(ObservableList<PaymentObligation> paymentObligations) {
        this(paymentObligations, Clock.systemDefaultZone());
    }

    /** Creates a payment-obligation panel using {@code clock} to derive overdue display states. */
    PaymentObligationListPanel(ObservableList<PaymentObligation> paymentObligations, Clock clock) {
        super(FXML, new BorderPane());
        this.paymentObligations = paymentObligations;
        this.clock = clock;
        filteredPayments = new FilteredList<>(paymentObligations, payment -> false);
        sortedPayments = new SortedList<>(filteredPayments,
                PaymentObligationOrdering.forSourceList(paymentObligations));
        paymentListView.setItems(sortedPayments);
        paymentListView.setCellFactory(unused -> createPaymentCell());
        sortedPayments.addListener((javafx.collections.ListChangeListener<PaymentObligation>) change ->
                updateEmptyState());
        updateEmptyState();
        Platform.runLater(this::scheduleNextPaymentStateRefresh);
    }

    /** Displays only the payment obligations belonging to {@code gig}. */
    public void showPaymentsFor(Gig gig) {
        selectedGig = gig;
        filteredPayments.setPredicate(payment -> isPaymentForGig(payment, gig));
        updateEmptyState();
    }

    /** Returns whether {@code payment} belongs to {@code gig}. */
    static boolean isPaymentForGig(PaymentObligation payment, Gig gig) {
        return PaymentObligationOrdering.isForGig(payment, gig);
    }

    /** Returns the payment list displayed by this panel. */
    ListView<PaymentObligation> getPaymentListView() {
        return paymentListView;
    }

    /** Returns the empty-state label used by this panel. */
    Label getEmptyState() {
        return emptyState;
    }

    /** Refreshes rendered payment states after the local date changes. */
    void refreshPaymentStates() {
        paymentListView.refresh();
    }

    /** Creates a list cell that derives its text and visual state from the displayed payment. */
    private ListCell<PaymentObligation> createPaymentCell() {
        ListCell<PaymentObligation> cell = new ListCell<>() {
            @Override
            protected void updateItem(PaymentObligation payment, boolean isEmpty) {
                super.updateItem(payment, isEmpty);
                getStyleClass().removeAll("payment-paid", "payment-outstanding", "payment-overdue");
                if (isEmpty || payment == null) {
                    setText(null);
                    return;
                }

                String displayState = getDisplayState(payment);
                setText(String.format("%d. Amount: $%s    Due: %s    %s",
                        getIndex() + 1, payment.getAmount(), payment.getDueDate(), displayState));
                getStyleClass().add("payment-" + displayState.toLowerCase(Locale.ROOT));
            }
        };
        cell.getStyleClass().add("payment-list-cell");
        cell.setWrapText(true);
        cell.prefWidthProperty().bind(paymentListView.widthProperty().subtract(20));
        return cell;
    }

    /** Returns the paid, outstanding, or overdue display state for {@code payment}. */
    private String getDisplayState(PaymentObligation payment) {
        if (payment.isPaid()) {
            return PAID;
        }
        return payment.getDueDate().getValue().isBefore(LocalDate.now(clock)) ? OVERDUE : OUTSTANDING;
    }

    /** Schedules the next payment-state refresh for the next midnight in the local time zone. */
    private void scheduleNextPaymentStateRefresh() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(clock);
        ZonedDateTime nextMidnight = currentDateTime.toLocalDate().plusDays(1).atStartOfDay(clock.getZone());
        long delayMillis = java.time.Duration.between(currentDateTime, nextMidnight).toMillis();
        paymentStateRefreshTimer.setDuration(javafx.util.Duration.millis(Math.max(1, delayMillis)));
        paymentStateRefreshTimer.setOnFinished(event -> {
            refreshPaymentStates();
            scheduleNextPaymentStateRefresh();
        });
        paymentStateRefreshTimer.playFromStart();
    }

    private void updateEmptyState() {
        emptyState.setText(selectedGig == null ? NO_GIG : (sortedPayments.isEmpty() ? NO_PAYMENTS : ""));
    }
}
