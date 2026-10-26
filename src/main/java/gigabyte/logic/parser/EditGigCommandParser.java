package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;

import java.util.Optional;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigTitle;

/**
 * Parses client and per-client gig indexes and optional replacement details.
 */
public class EditGigCommandParser implements Parser<EditGigCommand> {
    @Override
    public EditGigCommand parse(String args) throws ParseException {
        ArgumentMultimap fields = ArgumentTokenizer.tokenize(args, PREFIX_TITLE, PREFIX_DEADLINE,
                PREFIX_FEE, PREFIX_STATUS);
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditGigCommand.MESSAGE_USAGE);
        String[] indexes = fields.getPreamble().split("\\s+");
        if (indexes.length != 2 || fields.getValue(PREFIX_STATUS).isPresent()
                || (fields.getValue(PREFIX_TITLE).isEmpty() && fields.getValue(PREFIX_DEADLINE).isEmpty()
                && fields.getValue(PREFIX_FEE).isEmpty())) {
            throw new ParseException(usage);
        }
        fields.verifyNoDuplicatePrefixesFor(PREFIX_TITLE, PREFIX_DEADLINE, PREFIX_FEE);
        Index clientIndex;
        Index gigIndex;
        try {
            clientIndex = ParserUtil.parseIndex(indexes[0]);
            gigIndex = ParserUtil.parseIndex(indexes[1]);
        } catch (ParseException exception) {
            throw new ParseException(usage, exception);
        }
        Optional<GigTitle> title = fields.getValue(PREFIX_TITLE).isPresent()
                ? Optional.of(ParserUtil.parseGigTitle(fields.getValue(PREFIX_TITLE).get())) : Optional.empty();
        Optional<Deadline> deadline = fields.getValue(PREFIX_DEADLINE).isPresent()
                ? Optional.of(ParserUtil.parseDeadline(fields.getValue(PREFIX_DEADLINE).get())) : Optional.empty();
        Optional<Fee> fee = fields.getValue(PREFIX_FEE).isPresent()
                ? Optional.of(ParserUtil.parseFee(fields.getValue(PREFIX_FEE).get())) : Optional.empty();
        return new EditGigCommand(clientIndex, gigIndex, title, deadline, fee);
    }
}
