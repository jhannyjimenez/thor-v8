package org.mitre.thor.network.attack;

public class OrReqRule implements IReqRule{
    @Override
    public boolean isOption(Decision decision, Route route, AttackChain chain) {
        return decision.getRequirements().isEmpty() || decision.getRequirements().contains(route.getFullId());
    }
}
