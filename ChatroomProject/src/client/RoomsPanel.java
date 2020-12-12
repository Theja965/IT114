package client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

public class RoomsPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    List<RoomListItem> rooms = new ArrayList<RoomListItem>();
    JPanel container;
    JFrame parent;

    public RoomsPanel(JFrame frame) {
	parent = frame;
	container = new JPanel();
	JScrollPane scroll = new JScrollPane(container);
	container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
	container.setAlignmentY(TOP_ALIGNMENT);
	scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	JButton back = new JButton("Go Back");
	back.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		((ClientUI) parent).previous();
	    }
	});
	this.add(back);
	this.add(container);

    }

    public void addRoom(String room) {
	if (room != null) {
	    System.out.println("Adding: " + room);
	    RoomListItem r = new RoomListItem(room, (String roomName) -> handleSelection(roomName));
	    Dimension size = new Dimension(this.getSize().width, 40);
	    r.setPreferredSize(size);
	    r.setMaximumSize(size);
	    r.setMinimumSize(size);
	    container.add(r);
	    rooms.add(r);
	}
    }

    public void removeRoom(String room) {
	Iterator<RoomListItem> iter = rooms.iterator();
	while (iter.hasNext()) {
	    RoomListItem r = iter.next();
	    if (r.getRoomName().equalsIgnoreCase(room)) {
		r.close();
		r.removeAll();
		container.remove(r);
		iter.remove();
		break;
	    }
	}
	parent.invalidate();
	parent.repaint();
    }

    public void removeAllRooms() {
	System.out.println("Clearing rooms");
	Iterator<RoomListItem> iter = rooms.iterator();
	while (iter.hasNext()) {
	    RoomListItem r = iter.next();
	    System.out.println("Removing " + r.getRoomName());
	    container.remove(r);
	    r.close();
	    iter.remove();
	}
	parent.invalidate();
	parent.repaint();
    }

    public void handleSelection(String room) {
	SocketClient.INSTANCE.sendJoinRoom(room);
    }

}

