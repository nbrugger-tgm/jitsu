import * as vscode from 'vscode';
import { client } from './lsp';

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
	console.log('Congratulations, your extension "jitsu" is now active!');
	console.error("WEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE")
	client.start();
}

// This method is called when your extension is deactivated
export function deactivate(): Thenable<void> | undefined {
	if (!client) {
		return undefined;
	}
	return client.stop();
}