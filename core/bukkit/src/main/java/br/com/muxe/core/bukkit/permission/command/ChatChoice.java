package br.com.muxe.core.bukkit.permission.command;

import java.util.List;

public interface ChatChoice {

    String label();

    List<String> hover();

    String command();
}
