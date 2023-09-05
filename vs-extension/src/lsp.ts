import * as vscode from 'vscode';

import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';
import {Trace} from "vscode-languageclient";

const serverOptions: ServerOptions = {
    command: "/home/nils/workspace/priv/jitsu/language-server/build/install/language-server/bin/language-server",
    transport: TransportKind.stdio
};

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