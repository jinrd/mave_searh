document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chat-form');
    const messageInput = document.getElementById('message-input');
    const chatMessages = document.getElementById('chat-messages');

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const question = messageInput.value.trim();
        if (!question) return;

        // 1. Add user message to chat
        addMessage(question, 'user');
        messageInput.value = '';

        // 2. Show typing indicator
        const typingIndicator = addMessage('답변을 생성 중입니다...', 'typing');

        try {
            // 3. Call backend API
            const response = await fetch(`/api/manuals/semantic-search?question=${encodeURIComponent(question)}`);
            
            // 4. Remove typing indicator
            chatMessages.removeChild(typingIndicator);

            if (!response.ok) {
                const errorText = await response.text();
                addMessage(`오류가 발생했습니다: ${errorText}`, 'bot');
                return;
            }

            // 5. Add bot's answer to chat
            const answer = await response.text();
            addMessage(answer, 'bot');

        } catch (error) {
            if (chatMessages.contains(typingIndicator)) {
                chatMessages.removeChild(typingIndicator);
            }
            addMessage('서버와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', 'bot');
            console.error('Error:', error);
        }
    });

    /**
     * Adds a message to the chat window.
     * @param {string} text - The message content.
     * @param {string} type - The message type ('user', 'bot', or 'typing').
     */
    function addMessage(text, type) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('message');

        // Add classes based on the message type using if/else
        if (type === 'user') {
            messageDiv.classList.add('user-message');
        } else if (type === 'bot') {
            messageDiv.classList.add('bot-message');
        } else if (type === 'typing') {
            messageDiv.classList.add('bot-message', 'typing');
        }

        const p = document.createElement('p');
        p.textContent = text;
        
        messageDiv.appendChild(p);
        chatMessages.appendChild(messageDiv);

        // Scroll to the bottom
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        return messageDiv;
    }
});
