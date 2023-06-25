package State;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class IdentifiedDFSMState extends State {

	private String id;

	public IdentifiedDFSMState(String i) {
		this.id = i;
	}
	

	static public Set<String> parseStateIdList(String encoding) {
		Scanner scanner = new Scanner(encoding);
		
		Set<String> ids = new HashSet<String>();
		
		while(scanner.hasNext()) {
			ids.add(scanner.nextLine());
		}
		
		scanner.close();
		
		return ids;
	}
	
//	@Override
//	static public void prettyPrintStateSet(Collection<State> states, PrintStream out) {
//		
//		out.print("{");
//		
//		Iterator<State> p = states.iterator();
//		
//		if (p.hasNext()) {
//			p.next().prettyPrint(out);
//		}
//		
//		while(p.hasNext()) {
//			out.print(", ");
//			p.next().prettyPrint(out);
//		}
//		
//		out.print("}");
//	}

	public void prettyPrint(PrintStream out) {
		out.print(id);
	}

	public String toString() {
		return "id=" + id;
	}
	
	public String encode() {
		return "" + id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;		
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;		
		IdentifiedDFSMState other = (IdentifiedDFSMState) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public int compareTo(State other) {
		return id.compareTo(((IdentifiedDFSMState)other).id);
	}
}

