/**
 * 
 */
package com.i2r.ARC.PCControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Object handles a {@link DataResponse} object and performs some action based on what fields in the {@link DataResponse} object have been
 * filled.  This is probably not the best idea ever. 
 * 
 * This object has a reference back to the singleton {@link Controller}, so that it can access the {@link TaskStack}, {@link Controller#tasks}.  
 * 
 * Saving a file is set to be its own thread created by the inner {@link SaveFileRunnable}, so that the system does not block on File I/O.
 * 
 * @author Johnathan Pagnutti
 *
 *	TODO: instead of guessing what to do based on the format of a response action, keep a list of constant commands, and have each data
 *			response also keep track of what command it is tethered to
 */
public class ResponseAction {
	public static final String TASK_COMPLETE = "#";
	
	static final Logger logger = Logger.getLogger(ResponseAction.class);
	
	/**
	 * The reference to the {@link Controller} so that this object can access the {@link TaskStack}
	 */
	Controller cntrl;
	
	/**
	 * The {@link DataResponse} object that this action is going to use to attempt to do something
	 */
	DataResponse response;
	
	/**
	 * A {@link Task} that was referenced by the {@link DataResponse#taskID}.  Used here as a concrete reference to a particular task,
	 * even if, during processing, that task is removed from the {@link TaskStack}
	 */
	Task referencedTask;
	
	/**
	 * Constructor.  In addition to assigning the passed in value to the {@link ResponseAction#response} field, gets a reference to the
	 * {@link Controller} to use.
	 * 
	 * @param response the {@link DataResponse} object that contains the data to use to formulate a response
	 */
	public ResponseAction(DataResponse response){
		this.response = response;
		cntrl = Controller.getInstance();
	}
	
	/**
	 * Performs the action set in {@link ResponseAction#response}'s {@link DataResponse#type} with the data provided in the other fields of the 
	 * {@link ResponseAction#response} through a private method call in this {@link ResponseAction}.
	 * If the {@link DataResponse#type} is invalid, then don't do anything and log an error.
	 */
	public void performAction(){
		if(response.type == DataResponse.SAVE_FILE){
			saveFile();
		}else if (response.type == DataResponse.REMOVE_TASK){
			removeTask();
		}else{
			logger.error("The type " + response.type + " is invalid.");
		}
	}
	
	/**
	 * The action that removes a task from the stack.  This action should be called when {@link ResponseAction#response} follows the form
	 * of a {@link DataResponse} that would call for task removal.
	 */
	private void removeTask(){
		logger.debug("Removing a task with ID " + response.taskID + " from the stack.");
		
		referencedTask = cntrl.tasks.getTask(response.taskID);
		
		if(referencedTask != null){
			cntrl.tasks.removeTask(referencedTask.getId());
		}else{
			logger.error("Arrempted to remove a task with a reference to a task that was not on the stack");
		}
	}
	
	private void saveFile(){
		Thread t = new Thread(new SaveFileRunnable(response));
		t.start();
	}

	/****************
	 * INNER CLASS
	 ****************/
	
	private class SaveFileRunnable implements Runnable{
		
		private DataResponse saveResponse;
		
		public SaveFileRunnable(DataResponse response){
			this.saveResponse = response;
		}
		
		@Override
		public void run() {
			logger.debug("Saving file...");
			Task ref = cntrl.tasks.getTask(saveResponse.taskID);
			
			if(ref == null){
				//bad news bears
				logger.error("Arrempted to save a file with a reference to a task that was not on the stack");
				return;
			}
			
			File newFile = new File(ref.getId() + "_" + ++ref.pos + "." + ref.getCommand().getArguments().get(ARCCommand.PICTURE_FILETYPE_INDEX)); 
			
			logger.debug("File name: " + newFile.getName());
			try {
				FileOutputStream fileSave = new FileOutputStream(newFile);
				fileSave.write(saveResponse.fileData);
				fileSave.close();	
				logger.debug("File saved.");
			} catch (FileNotFoundException e) {
				logger.debug(e.getMessage(), e);
				e.printStackTrace();
			} catch (IOException e) {
				logger.debug(e.getMessage(), e);
				e.printStackTrace();
			}
		}
	}
}
