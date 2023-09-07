import * as vscode from 'vscode';
import { client } from './lsp';

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
	console.log("jitsu activated")
	const command = 'jistu.connectLsp';

	const startLsp = () => {
		client.start()
	};

	context.subscriptions.push(vscode.commands.registerCommand(command, startLsp));
	client.start();
}

// This method is called when your extension is deactivated
export function deactivate(): Thenable<void> | undefined {
	if (!client) {
		return undefined;
	}
	return client.stop();
}