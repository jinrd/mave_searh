document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chat-form');
    const messageInput = document.getElementById('message-input');
    const chatMessages = document.getElementById('chat-messages');

    // 대화 ID를 저장할 변수
    let currentConversationId = null;

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const question = messageInput.value.trim();
        if (!question) return;

        addMessage(question, 'user');
        messageInput.value = '';
        const typingIndicator = addMessage('답변을 생성 중입니다...', 'typing');

        try {
            // API URL에 conversationId 추가
            let apiUrl = `/api/manuals/semantic-search?question=${encodeURIComponent(question)}`;
            if (currentConversationId) {
                apiUrl += `&conversationId=${currentConversationId}`;
            }

            const response = await fetch(apiUrl);
            chatMessages.removeChild(typingIndicator);

            if (!response.ok) { throw new Error('서버 응답 오류'); }

            // JSON 응답 처리
            const data = await response.json();
            const answer = data.answer;
            
            // 서버로부터 받은 conversationId를 저장
            currentConversationId = data.conversationId;

            addMessage(answer, 'bot');

        } catch (error) {
            if (chatMessages.contains(typingIndicator)) {
                chatMessages.removeChild(typingIndicator);
            }
            addMessage('서버와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', 'bot');
            console.error('Error:', error);
        }
    });

    function addMessage(text, type) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('message');

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
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return messageDiv;
    }
});