package com.arsenal.lebanon.manager.model;

import java.util.Set;

public enum MemberType {
    President,
    Secretary,
    Treasurer,
    Board,
    Permanent,
    Default;

    public static final Set<MemberType> ADMIN_TYPES = Set.of(President, Secretary, Treasurer);
    public static final Set<MemberType> BOARD_TYPES = Set.of(President, Secretary, Treasurer, Board);
}
