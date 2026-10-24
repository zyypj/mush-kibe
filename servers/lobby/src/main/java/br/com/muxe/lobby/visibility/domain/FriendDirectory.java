package br.com.muxe.lobby.visibility.domain;

import java.util.UUID;

@FunctionalInterface
public interface FriendDirectory {

    FriendDirectory NONE = (player, other) -> false;

    boolean areFriends(UUID player, UUID other);
}
