# Pocket AI 🤖💙

### Your personal AI. Always in your pocket.

Pocket AI is an open-source Android application designed to bring private, on-device AI conversations directly to your phone. Chat with your own AI model, explore ideas, ask questions, and work with your files — without relying on cloud AI services.

Our goal is to create a friendly, personal AI companion that runs locally, keeps your data on your device, and gives you control over your AI experience.

> **Private by design. Local by default. Always yours.**

---

## ✨ Features

### 💬 Personal AI Chat
- A simple, friendly, conversational interface.
- One continuous conversation with your AI assistant.
- Locally saved conversation history.
- Customizable assistant personality and response style.
- Support for formatted responses and code snippets.
- Stop and regenerate responses when supported by the inference engine.

### 🧠 Run Your Own AI Models
- Bring your own compatible AI model.
- Select model files from your device storage.
- Support for compatible GGUF models through a suitable local inference engine.
- Load and unload models when needed.
- Display model status, file information, and errors.
- No automatic model downloads.
- No mandatory accounts or subscriptions.

Model compatibility and performance depend on the model architecture, available RAM, processor, and the inference engine.

### 📎 Chat With Your Files
Pocket AI is designed to support local attachments, including:

- Images: PNG, JPEG, WebP, and other supported formats.
- Documents: PDF, DOC, and DOCX.
- Presentations: PPT and PPTX.
- Spreadsheets: XLS, XLSX, and CSV.
- Text files: TXT and Markdown.
- Videos: MP4 and other supported formats.

Text extraction and attachment understanding depend on the available local processing components. Image understanding requires a compatible vision model; video analysis may require local frame extraction and speech recognition.

### 🎨 Personalization
- Customize the assistant's name.
- Choose your preferred response language.
- Configure tone and response length.
- Add custom instructions.
- Save preferences locally.
- Choose light, dark, or system appearance.

### 🔒 Privacy First
- Designed for on-device inference.
- No cloud AI API required for local conversations.
- No automatic uploading of chats or attachments.
- No mandatory account.
- Local conversation and preference storage.
- User-controlled model selection.

Pocket AI aims to work entirely offline during normal use once the application and its required components are installed. Network isolation and third-party dependency behavior must be verified during development and testing.

---

## 🛠️ Technology Stack

The project is designed around the following technologies:

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | Modern declarative UI |
| Material 3 | UI components and theming |
| MVVM | Presentation architecture |
| Kotlin Coroutines & Flow | Asynchronous processing |
| Room | Local conversation database |
| DataStore | Persistent preferences |
| Android Storage Access Framework | Model and attachment selection |
| llama.cpp | Potential local inference engine for compatible GGUF models |

The exact dependencies and supported model formats will be documented as implementation progresses.

---

## 🚀 Getting Started

### Requirements

- Android Studio or a compatible Android development environment.
- Android SDK and a compatible JDK.
- A physical Android device or emulator.
- Sufficient storage for the application and selected model.
- A compatible local AI model for actual AI conversations.

### Build From Source

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/Pocket-AI.git
```

**2. Open the project**

Open the cloned folder in Android Studio or your compatible IDE.

**3. Sync Gradle**

Allow Gradle to download the required build dependencies. This initial setup may require an internet connection.

**4. Build the debug APK**

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew assembleDebug
```

**5. Locate the APK**

The standard output location is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

These instructions assume the project uses the standard Android Gradle structure.

---

## 📦 Model Setup

Pocket AI follows a bring-your-own-model approach.

1. Obtain a compatible model separately.
2. Open Pocket AI's model settings.
3. Select the model file or supported model directory.
4. Allow the app to validate the selected resource.
5. Load the model.
6. Start a conversation.

Large language models can require several gigabytes of storage and memory. Start with a small, quantized model appropriate for your device.

Do not assume every GGUF file is a chat model. Embedding models, unsupported architectures, and vision models may require different runtime support.

Pocket AI will not automatically download a model on your behalf.

---

## 🗺️ Roadmap

Our development plan includes:

- [ ] Initial Android project and app identity.
- [ ] Chat interface and message composer.
- [ ] Persistent single-conversation history.
- [ ] Local preferences and assistant customization.
- [ ] Model file selection and management.
- [ ] Integration with a compatible local inference engine.
- [ ] Streaming responses and generation cancellation.
- [ ] Local document text extraction.
- [ ] Image attachment support.
- [ ] PDF and Office document processing.
- [ ] Local video frame extraction and transcription.
- [ ] Improved memory management and performance.
- [ ] Local document indexing and retrieval-augmented generation (RAG).
- [ ] Optional user-controlled persistent AI memory.
- [ ] Automated tests and release builds.

Features will be marked complete only after they are implemented and tested.

---

## 🏗️ Project Principles

Pocket AI is being developed around five principles:

1. **Privacy:** personal conversations should stay on the user's device.
2. **Ownership:** users choose their own compatible models.
3. **Simplicity:** the main experience is one continuous personal conversation.
4. **Transparency:** unsupported features and errors should be communicated honestly.
5. **Extensibility:** future capabilities should fit into a modular architecture.

---

## 🤝 Contributing

Pocket AI is being developed collaboratively.

Contributions, bug reports, feature suggestions, and documentation improvements are welcome.

### Contribution workflow

1. Fork the repository or create a feature branch.
2. Make a focused change.
3. Run the available tests and build checks.
4. Submit a pull request describing the change.
5. Address review feedback before merging.

Example:

```bash
git switch -c feature/my-feature
git add .
git commit -m "Add my feature"
git push -u origin feature/my-feature
```

Please avoid committing model weights, personal conversations, local configuration files, signing keys, or other sensitive data.

---

## 🔐 Privacy and Security

Pocket AI is intended to keep AI processing and user data local.

The privacy model depends on the implemented application, inference engine, and dependencies. Before a release, network behavior, storage permissions, file handling, and third-party libraries should be reviewed and tested.

Never place secrets or private user data in source control.

---

## 📄 License

The project license has not yet been selected. A license will be added before the first public release so that contributors and users understand their rights and obligations.

---

## 💙 Our Vision

We believe everyone should have access to a personal AI assistant without having to surrender their conversations or depend on remote services.

Pocket AI is our attempt to make local AI more approachable, useful, and personal — one conversation at a time.

**Made with curiosity, code, and a little robot magic. 🤖**

