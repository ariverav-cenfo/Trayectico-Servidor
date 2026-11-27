package com.example.trayecticoservidor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

public class ReasoningFilteringChatModel implements ChatModel {

    private static final String REASONING_CONTENT = "reasoningContent";

    private final ChatModel delegate;

    public ReasoningFilteringChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.call(filterPrompt(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(filterPrompt(prompt));
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    private Prompt filterPrompt(Prompt prompt) {

        List<Message> filteredMessages = new ArrayList<>();

        for (Message message : prompt.getInstructions()) {

            if (message instanceof AssistantMessage assistantMessage) {

                Map<String, Object> metadata =
                        new HashMap<>(assistantMessage.getMetadata());

                metadata.remove(REASONING_CONTENT);

                AssistantMessage filteredMessage =
                        assistantMessage.mutate()
                                .properties(metadata)
                                .build();

                filteredMessages.add(filteredMessage);

            } else {
                filteredMessages.add(message);
            }
        }

        return new Prompt(filteredMessages, prompt.getOptions());
    }
}
