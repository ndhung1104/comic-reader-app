package com.group09.ComicReader.ui.chatbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ChatMessageAdapter;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.ChatMessage;

public class ChatbotAssistantView extends LinearLayout {

    private CardView cardChatWindow;
    private View fabChatButton;
    private RecyclerView recyclerMessages;
    private EditText etInput;
    private ImageButton btnSend;
    private ProgressBar progressLoading;
    private TextView tvHeaderTitle;
    private ImageButton btnClose;

    private ChatMessageAdapter adapter;
    private boolean isOpen = false;
    private boolean isLoading = false;

    public ChatbotAssistantView(@NonNull Context context) {
        this(context, null);
    }

    public ChatbotAssistantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatbotAssistantView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_chatbot_assistant, this, true);

        cardChatWindow = findViewById(R.id.card_chat_window);
        fabChatButton = findViewById(R.id.fab_chat_button);
        recyclerMessages = findViewById(R.id.recycler_chat_messages);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);
        progressLoading = findViewById(R.id.progress_chat_loading);
        tvHeaderTitle = findViewById(R.id.tv_chat_header_title);
        btnClose = findViewById(R.id.btn_chat_close);

        adapter = new ChatMessageAdapter();
        recyclerMessages.setLayoutManager(new LinearLayoutManager(context));
        recyclerMessages.setAdapter(adapter);

        // Add welcome message
        adapter.addMessage(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                context.getString(R.string.chatbot_welcome)));

        fabChatButton.setOnClickListener(v -> openChat());
        btnClose.setOnClickListener(v -> closeChat());
        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    public void openChat() {
        if (isOpen) return;
        isOpen = true;
        fabChatButton.setVisibility(GONE);
        cardChatWindow.setVisibility(VISIBLE);
        cardChatWindow.setAlpha(0f);
        cardChatWindow.setScaleX(0.8f);
        cardChatWindow.setScaleY(0.8f);

        ObjectAnimator animAlpha = ObjectAnimator.ofFloat(cardChatWindow, "alpha", 0f, 1f);
        ObjectAnimator animScaleX = ObjectAnimator.ofFloat(cardChatWindow, "scaleX", 0.8f, 1f);
        ObjectAnimator animScaleY = ObjectAnimator.ofFloat(cardChatWindow, "scaleY", 0.8f, 1f);

        animAlpha.setDuration(250);
        animScaleX.setDuration(250);
        animScaleY.setDuration(250);

        animAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        animScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        animScaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        animAlpha.start();
        animScaleX.start();
        animScaleY.start();
    }

    public void closeChat() {
        if (!isOpen) return;
        isOpen = false;

        ObjectAnimator animAlpha = ObjectAnimator.ofFloat(cardChatWindow, "alpha", 1f, 0f);
        ObjectAnimator animScaleX = ObjectAnimator.ofFloat(cardChatWindow, "scaleX", 1f, 0.8f);
        ObjectAnimator animScaleY = ObjectAnimator.ofFloat(cardChatWindow, "scaleY", 1f, 0.8f);

        animAlpha.setDuration(200);
        animScaleX.setDuration(200);
        animScaleY.setDuration(200);

        animAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        animScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        animScaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        animAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardChatWindow.setVisibility(GONE);
                fabChatButton.setVisibility(VISIBLE);
            }
        });

        animAlpha.start();
        animScaleX.start();
        animScaleY.start();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean onBackPressed() {
        if (isOpen) {
            closeChat();
            return true;
        }
        return false;
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || isLoading) return;

        etInput.setText("");
        adapter.addMessage(new ChatMessage(ChatMessage.ROLE_USER, text));
        scrollToBottom();

        // Add loading indicator
        adapter.addMessage(new ChatMessage(ChatMessage.ROLE_ASSISTANT, "", true));
        isLoading = true;
        progressLoading.setVisibility(VISIBLE);
        btnSend.setEnabled(false);
        scrollToBottom();

        ComicRepository.getInstance().chatWithAssistant(text, new ComicRepository.ChatCallback() {
            @Override
            public void onSuccess(String reply) {
                post(() -> {
                    adapter.removeLoading();
                    adapter.addMessage(new ChatMessage(ChatMessage.ROLE_ASSISTANT, reply));
                    isLoading = false;
                    progressLoading.setVisibility(GONE);
                    btnSend.setEnabled(true);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                post(() -> {
                    adapter.removeLoading();
                    adapter.addMessage(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                            getContext().getString(R.string.chatbot_error, error)));
                    isLoading = false;
                    progressLoading.setVisibility(GONE);
                    btnSend.setEnabled(true);
                    scrollToBottom();
                });
            }
        });
    }

    private void scrollToBottom() {
        recyclerMessages.post(() -> {
            if (adapter.getItemCount() > 0) {
                recyclerMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    public void setVisible(boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
    }
}

