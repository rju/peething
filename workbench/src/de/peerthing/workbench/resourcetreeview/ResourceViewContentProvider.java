package de.peerthing.workbench.resourcetreeview;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.peerthing.workbench.filetyperegistration.IFileTypeRegistration;



/**
 * A Content provider used for the tree view in the resource view.
 * 
 * @author Michael Gottschalk
 * 
 */
public class ResourceViewContentProvider implements ITreeContentProvider {
    private ArrayList<IFileTypeRegistration> fileTypeRegistration;

    private Hashtable<String, ArrayList<IFileTypeRegistration>> filetypes;

    /**
     * Creates a new content provider.
     * 
     * @param fileTypeRegistration
     *            The registered filetypes contributed by other plug-ins
     * @param filetypes
     *            The filetype registrations ordered by filenames they support.
     */
    public ResourceViewContentProvider(
            ArrayList<IFileTypeRegistration> fileTypeRegistration,
            Hashtable<String, ArrayList<IFileTypeRegistration>> filetypes) {
        this.filetypes = filetypes;
        this.fileTypeRegistration = fileTypeRegistration;
    }

    /**
     * not used
     */
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    /**
     * not used
     */
    public void dispose() {
    }

    /**
     * Returns the list of projects given a workspace root.
     */
    public Object[] getElements(Object parent) {
        IProject[] projects = ((IWorkspaceRoot) parent).getProjects();
        return projects;
    }

    /**
     * Always returns null since it is not really needed by the tree viewer.
     * 
     */
    public Object getParent(Object child) {
        return null;
    }

    /**
     * Returns the children of projects, folders and of files. The contents of
     * files are taken from the filetype registrations provided by other
     * plug-ins.
     * 
     */
    public Object[] getChildren(Object parent) {
        if (parent instanceof IContainer) {
            try {
                IResource[] resources = ((IContainer) parent).members();
                Vector<Object> visibleRes = new Vector<Object>();
                for (int i = 0; i < resources.length; i++) {
                    // Don't show project files and
                    // files generated by a hsql-DB.
                    if (resources[i] instanceof IContainer) {
                        visibleRes.add(resources[i]);
                    } else if (resources[i].getName() != null
                            && resources[i].getFileExtension() != null
                            && !resources[i].getName().equals(".project")
                            && !resources[i].getFileExtension()
                                    .equals("script")
                            && !resources[i].getFileExtension().equals(
                                    "properties")
                            && !resources[i].getFileExtension().equals("log")
                            && !resources[i].getFileExtension().equals("lck")) {
                        visibleRes.add(resources[i]);
                    }
                }

                return visibleRes.toArray();
            } catch (CoreException e) {
                e.printStackTrace();
            }
        } else if (parent instanceof IFile) {
            ArrayList<IFileTypeRegistration> regs = filetypes
                    .get(((IFile) parent).getFileExtension());
            if (regs != null) {
                for (IFileTypeRegistration reg : regs) {
                    if (reg.hasSubTree((IFile) parent)) {
                        return reg.getTreeElements((IFile) parent);
                    }
                }
            }
        }

        for (IFileTypeRegistration reg : fileTypeRegistration) {
            if (reg.canHandleSubtreeObject(parent)) {
                return reg.getSubtreeContentProvider().getChildren(parent);
            }
        }

        return null;
    }

    /**
     * Returns if a file, project, folder or object beneath a file has children.
     * File tree information is taken from the filetype registrations provided
     * by plug-ins.
     * 
     */
    public boolean hasChildren(Object parent) {
        if (parent instanceof IContainer) {
            return getChildren(parent).length > 0;
        } else if (parent instanceof IFile) {
            IFile file = (IFile) parent;
            boolean hasSubtree = false;
            if (file.getFileExtension() != null
                    && filetypes.containsKey(file.getFileExtension())) {
                for (IFileTypeRegistration reg : filetypes.get(file
                        .getFileExtension())) {
                    if (reg.hasSubTree(file)) {
                        hasSubtree = true;
                    }
                }
            }
            return hasSubtree;
        } else {
            // look if it is part of a subtree provided
            // by another component
            for (IFileTypeRegistration reg : fileTypeRegistration) {
                if (reg.canHandleSubtreeObject(parent)) {
                    return reg.getSubtreeContentProvider().hasChildren(parent);
                }
            }
        }

        return false;
    }

}