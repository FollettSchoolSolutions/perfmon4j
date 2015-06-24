package web.org.perfmon4j.console.app.zk;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.SelectorComposer;

@SuppressWarnings("serial")
public abstract class RefreshableComposer<T extends Component> extends
		SelectorComposer<T> {

	@Override
	public void doAfterCompose(T comp) throws Exception {
		super.doAfterCompose(comp);

		RefreshListener<T> refreshListener = new RefreshListener<T>(comp);
		EventQueue<Event> eq = EventQueues.lookup("interactive", EventQueues.DESKTOP, true);
        eq.subscribe(refreshListener);
	}
	
	static public void postRefreshEvent(Component parent) {
		postRefreshEvent(parent, "default");
	}

	static public void postRefreshEvent(Component parent, String eventName) {
		if (parent != null) {
			EventQueue<Event> eq = EventQueues.lookup("interactive", EventQueues.DESKTOP, true);
			eq.publish(new Event(eventName, parent, "refresh event"));
		}
	}
	
	protected abstract void handleRefreshEvent(Event event);

	private class RefreshListener<T2 extends Component> implements
			EventListener<Event> {
		private final T2 component;

		public RefreshListener(T2 component) {
			this.component = component;
		}

		@Override
		public void onEvent(Event event) throws Exception {
			if (component.equals(event.getTarget())) {
				handleRefreshEvent(event);
			}
		}
	}
}
