package me.tinyoverflow.griefprevention;

public class Permissions
{
    public static class Commands
    {
        public static final String CLAIM = "griefprevention.command.claim";
        public static final String CLAIM_ADMIN = "griefprevention.command.claimadmin";
        public static final String TOOLMODE = "griefprevention.command.toolmode";

        public static class ClaimAdmin
        {
            public static final String LIST = "griefprevention.command.claimadmin.list";
            public static final String IGNORE = "griefprevention.command.claimadmin.ignore";
            public static final String DELETE_ALL = "griefprevention.command.claimadmin.delete-all";
        }

        public static class ToolMode
        {
            public static final String ADMIN = "griefprevention.command.toolmode.admin";
            public static final String BASIC = "griefprevention.command.toolmode.basic";
            public static final String SUBDIVIDE = "griefprevention.command.toolmode.subdivide";
            public static final String RESTORE_NATURE = "griefprevention.command.toolmode.restore-nature";
            public static final String RESTORE_NATURE_AGGRESSIVE = "griefprevention.command.toolmode.restore-nature-aggressive";
            public static final String RESTORE_NATURE_FILL = "griefprevention.command.toolmode.restore-nature-fill";
        }
    }

    public static class Claim
    {
        public static final String OVERRIDE_LIMIT = "griefprevention.claim.override-limit";
    }
}
