package org.mitre.thor.network.attack;

public enum RequirementsRule {
    OR(new OrReqRule()),
    AND(new AndReqRule());

    private final IReqRule reqRule;

    public boolean isOption(Decision decision, Route route, AttackChain chain) {
        return this.reqRule.isOption(decision, route, chain);
    }

    RequirementsRule(IReqRule reqRule){
        this.reqRule = reqRule;
    }
}
