import {LanguageClient, LanguageClientOptions, State, StreamInfo} from 'vscode-languageclient/node';
import {Trace} from "vscode-languageclient";
import * as net from "net";

let connectionInfo = {
    port: 5007,
    host: "localhost"
};

let serverOptions = () => {
    // Connect to language server via socket
    let socket = net.connect(connectionInfo);
    let result: StreamInfo = {
        writer: socket,
        reader: socket
    };
    return Promise.resolve(result);
};
// const serverOptions: ServerOptions = {
//     command: "",
//     // command: "/home/nils/workspace/priv/jitsu/language-server/build/install/language-server/bin/language-server",
//     transport: TransportKind.stdio
//
// };

const clientOptions: LanguageClientOptions = {
    documentSelector: [
        // Active functionality on files of these languages.
        {
            language: 'jitsu',
        }
    ],
};

export const client = new LanguageClient('jitsu-lsp', serverOptions, clientOptions);
client.setTrace(Trace.Verbose);

var restarts = 2;
client.onDidChangeState(ev => {
    if(ev.newState == State.Stopped) {
        if(restarts == 0) {
            console.log("Maximum restarts reached.")
            return
        }
        client.restart().then(() => restarts = 2).catch(() => restarts--)
    } else if(ev.newState == State.Running){
        restarts = 1;
    }
})