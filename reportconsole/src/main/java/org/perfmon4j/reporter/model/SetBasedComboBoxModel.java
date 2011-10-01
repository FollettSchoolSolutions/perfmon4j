package org.perfmon4j.reporter.model;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;


/**
 * Note this is not implemented with a Set... Because we need to
 * maintain the order of the list.  However like a Set it will
 * not allow duplicate entries.
 */
public class SetBasedComboBoxModel<T extends Object> implements MutableComboBoxModel {
	public final Vector<T> items;
	public final MutableComboBoxModel delegate;
	
	public SetBasedComboBoxModel() {
		this.items = new Vector<T>();
		this.delegate = new DefaultComboBoxModel(items);
	}

	public void addElement(Object anItem) {
		if (!items.contains(anItem)) {
			delegate.addElement((T)anItem);
		} else {
			setSelectedItem(anItem);
		}
	}
	
	public T getSelectedItem() {
		return (T)delegate.getSelectedItem();
	}

	public void setSelectedItem(Object anItem) {
		delegate.setSelectedItem((T)anItem);
	}

	public T getElementAt(int index) {
		return (T)delegate.getElementAt(index);
	}

	public int getSize() {
		return delegate.getSize();
	}

	public void addListDataListener(ListDataListener l) {
		delegate.addListDataListener(l);
	}
	
	public void removeListDataListener(ListDataListener l) {
		delegate.removeListDataListener(l);
	}
	
	public Iterator<T> iterator() {
		return items.iterator();
	}

	public void insertElementAt(Object anItem, int index) {
		if (items.contains(anItem)) {
			throw new RuntimeException("Duplicate items not allowed");
		} else {
			delegate.insertElementAt(anItem, index);
		}
	}

	public void removeElement(Object anItem) {
		delegate.removeElement(anItem);
	}

	public void removeElementAt(int index) {
		delegate.removeElementAt(index);
	}
}
