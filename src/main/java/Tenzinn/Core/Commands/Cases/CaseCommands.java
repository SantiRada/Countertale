package Tenzinn.Core.Commands.Cases;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class CaseCommands extends AbstractCommandCollection {

    public CaseCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new OpenCaseCommand      ("open",      "Open roulette UI with a random or specific skin"));
        addSubCommand(new DropCaseCommand      ("drop",      "Add one or more cases to inventory (--amount=N)"));
        addSubCommand(new GiveSkinCommand      ("give",      "Add a skin to inventory directly (no animation)"));
        addSubCommand(new ListCasesCommand     ("inventory", "List all skins in your in-memory inventory"));
        addSubCommand(new ClearCasesCommand    ("clear",     "Clear your in-memory skin inventory"));
        addSubCommand(new SetCaseChanceCommand ("chance",    "Set the case drop chance (0-100)"));
    }

    @Override
    public String getPermission() { return "countertale.cases"; }

    @Override
    public String getName() { return "case"; }
}
