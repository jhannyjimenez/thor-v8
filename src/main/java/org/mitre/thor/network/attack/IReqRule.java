package org.mitre.thor.network.attack;

public interface IReqRule {
    boolean isOption(Decision decision, Route route, AttackChain chain);
}
