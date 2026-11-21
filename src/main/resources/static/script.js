 const form = document.getElementById('chatForm');
    const questionInput = document.getElementById('question');
    const messages = document.getElementById('messages');
    const sendButton = document.getElementById('sendButton');

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        await sendQuestion(questionInput.value.trim());
    });

    async function askQuick(question) {
        questionInput.value = question;
        await sendQuestion(question);
    }

    async function sendQuestion(question) {
        if (!question) return;

        addMessage(question, 'user');
        questionInput.value = '';
        sendButton.disabled = true;

        const typing = addMessage('Searching company policies...', 'assistant typing');

        try {
            const response = await fetch('/api/v1/query?question=' + encodeURIComponent(question));
            const data = await response.json();
            typing.remove();
            addMessage(data.answer || 'No answer received.', 'assistant');
        } catch (error) {
            typing.remove();
            addMessage('Sorry, I could not reach the support service.', 'assistant');
        } finally {
            sendButton.disabled = false;
            questionInput.focus();
        }
    }

    function addMessage(text, className) {
        const div = document.createElement('div');
        div.className = 'message ' + className;
        div.textContent = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
        return div;
    }