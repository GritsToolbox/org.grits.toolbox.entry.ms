package org.grits.toolbox.entry.ms.command;

import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.entry.ms.handler.ViewMassSpecResults;

@SuppressWarnings("restriction")
public class ViewMassSpecOverviewCommandExecutor  {
	
	public static void showMSOverview(IEclipseContext context, Entry entry ) {	
		ECommandService commandService = context.get(ECommandService.class);
		EHandlerService handlerService = context.get(EHandlerService.class);
		
		context.set(ViewMassSpecResults.PARAMETER_ID, entry);
		handlerService.executeHandler(
			commandService.createCommand(ViewMassSpecResults.COMMAND_ID, null));		
	}
}
