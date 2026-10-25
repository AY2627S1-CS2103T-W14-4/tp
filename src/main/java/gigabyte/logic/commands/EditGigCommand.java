package gigabyte.logic.commands;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.ToStringBuilder;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigOrdering;
import gigabyte.model.gig.GigTitle;

/**
 * Edits a gig's details without changing its identity, status, or linked payments.
 */
public class EditGigCommand extends Command {
    public static final String COMMAND_WORD = "editgig";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits a gig's title, deadline, or agreed fee.\n"
            + "Parameters: CLIENT_INDEX GIG_INDEX [t/TITLE] [d/DEADLINE] [f/FEE]\n"
            + "Provide at least one field. Use setstatus to change status.\n"
            + "Example: " + COMMAND_WORD + " 1 2 t/Updated landing page d/2027-04-30 f/1800.00";
    public static final String MESSAGE_NOT_EDITED = "The gig already has the specified details.";

    private final Index clientIndex;
    private final Index gigIndex;
    private final Optional<GigTitle> title;
    private final Optional<Deadline> deadline;
    private final Optional<Fee> fee;

    /**
     * Creates a command applying at least one replacement detail to the indexed gig.
     */
    public EditGigCommand(Index clientIndex, Index gigIndex, Optional<GigTitle> title,
            Optional<Deadline> deadline, Optional<Fee> fee) {
        requireAllNonNull(clientIndex, gigIndex, title, deadline, fee);
        if (title.isEmpty() && deadline.isEmpty() && fee.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_USAGE);
        }
        this.clientIndex = clientIndex;
        this.gigIndex = gigIndex;
        this.title = title;
        this.deadline = deadline;
        this.fee = fee;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Client> clients = model.getFilteredClientList();
        if (clientIndex.getZeroBased() >= clients.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX);
        }
        Client client = clients.get(clientIndex.getZeroBased());
        List<Gig> gigs = GigOrdering.forClient(model.getGigList(), client);
        if (gigIndex.getZeroBased() >= gigs.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_GIG_INDEX);
        }
        Gig target = gigs.get(gigIndex.getZeroBased());
        Gig replacement = target.withDetails(title.orElse(target.getTitle()),
                deadline.orElse(target.getDeadline()), fee.orElse(target.getAgreedFee()));
        if (target.equals(replacement)) {
            throw new CommandException(MESSAGE_NOT_EDITED);
        }
        model.setGig(target, replacement);
        return new CommandResult(String.format("Edited gig %d for %s: Title: %s; Deadline: %s; Agreed fee: %s",
                gigIndex.getOneBased(), client.getName(), replacement.getTitle(),
                replacement.getDeadline(), replacement.getAgreedFee()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof EditGigCommand command && clientIndex.equals(command.clientIndex)
                && gigIndex.equals(command.gigIndex) && title.equals(command.title)
                && deadline.equals(command.deadline) && fee.equals(command.fee);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("clientIndex", clientIndex).add("gigIndex", gigIndex)
                .add("title", title).add("deadline", deadline).add("fee", fee).toString();
    }
}
