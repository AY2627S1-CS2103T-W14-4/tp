package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;

import java.util.List;
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
        if (fields.getValue(PREFIX_STATUS).isPresent()
                || (fields.getValue(PREFIX_TITLE).isEmpty() && fields.getValue(PREFIX_DEADLINE).isEmpty()
                && fields.getValue(PREFIX_FEE).isEmpty())) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditGigCommand.MESSAGE_USAGE));
        }
        List<Index> indexes = ParserUtil.parseIndexes(fields.getPreamble(), 2, EditGigCommand.MESSAGE_USAGE);
        fields.verifyNoDuplicatePrefixesFor(PREFIX_TITLE, PREFIX_DEADLINE, PREFIX_FEE);
        Optional<GigTitle> title = fields.getValue(PREFIX_TITLE).isPresent()
                ? Optional.of(ParserUtil.parseGigTitle(fields.getValue(PREFIX_TITLE).get())) : Optional.empty();
        Optional<Deadline> deadline = fields.getValue(PREFIX_DEADLINE).isPresent()
                ? Optional.of(ParserUtil.parseDeadline(fields.getValue(PREFIX_DEADLINE).get())) : Optional.empty();
        Optional<Fee> fee = fields.getValue(PREFIX_FEE).isPresent()
                ? Optional.of(ParserUtil.parseFee(fields.getValue(PREFIX_FEE).get())) : Optional.empty();
        return new EditGigCommand(indexes.get(0), indexes.get(1), title, deadline, fee);
    }
}
