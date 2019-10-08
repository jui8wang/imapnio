package com.yahoo.imapnio.async.request;

/**
 * This class defines IMAP NAMESPACE command request from client.
 */
public class NamespaceCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String NAMESPACE = "NAMESPACE";

    /**
     * Initializes the @{code NamespaceCommand}.
     */
    public NamespaceCommand() {
        super(NAMESPACE);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.NAMESPACE;
    }
}
