package org.mitre.thor.network.attack;

public enum RequirementsRule {
    OR(new OrReqRule()),
    AND(new AndReqRule());

    private final IReqRule reqRule;

    public boolean isOption(Decision decision, AttackChain chain) {
        return this.reqRule.isOption(decision, chain);
    }

    RequirementsRule(IReqRule reqRule){
        this.reqRule = reqRule;
    }
}
