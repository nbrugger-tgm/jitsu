

## Folder structure

| folder            | description                                                                                      |
|-------------------|--------------------------------------------------------------------------------------------------|
| `backend/<name>`  | A backend that jitsu can be compile to                                                           |
| `cli`             | The executable cli to compile jitsu                                                              |
| `buildSrc`        | Shared gradle & build logic                                                                      |
| `compiler`        | The library containing the code of the compiler itself                                           |
| `language-server` | The language server for jitsu development                                                        |
| `playground`      | A webpage that allows compiling jitsu online and inspecting the result                           |
| `vs-extension`    | The extension for VisualStudio Code registering the language server                              |
| `gradle-plugin`   | Since gradle is the official build tool for jitsu this is the official plugin for jitsu projects |