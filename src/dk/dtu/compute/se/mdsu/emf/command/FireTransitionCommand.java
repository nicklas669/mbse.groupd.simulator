package dk.dtu.compute.se.mdsu.emf.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.CreateChildCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

import dk.dtu.compute.se.mdsu.petrinet.Arc;
import dk.dtu.compute.se.mdsu.petrinet.Node;
import dk.dtu.compute.se.mdsu.petrinet.PetrinetFactory;
import dk.dtu.compute.se.mdsu.petrinet.PetrinetPackage;
import dk.dtu.compute.se.mdsu.petrinet.Place;
import dk.dtu.compute.se.mdsu.petrinet.Token;
import dk.dtu.compute.se.mdsu.petrinet.Transition;

public class FireTransitionCommand extends CompoundCommand {
	
	public FireTransitionCommand(EditingDomain domain, Transition transition) {
		super("Fire Transition");

		// compute the number of tokens needed for each place
		// note that some places might occur twice in the preset, which is why
		// we use the map needed to sum up the number of needed tokens first
		Map<Place, Integer> needed = new HashMap<Place,Integer>();
		for (Arc arc: transition.getIn()) {
			Node node = arc.getSource();
			if (node instanceof Place) {
				Place source = (Place) node;
				if (needed.containsKey(source)) {
					needed.put(source, needed.get(source) + 1);
				} else {
					needed.put(source, 1);
				}
			}
		}
		
		// add commands for removing the needed number of tokens from each place
		for (Place place: needed.keySet()) {
			for (int i = 0; i < needed.get(place) && i < place.getTokens().size(); i++)
				this.append(new RemoveCommand(domain, place, PetrinetPackage.eINSTANCE.getPlace_Tokens(), place.getTokens().get(i)));
		}
		
		// add commands for adding a token to each target place of each output arc
		for (Arc arc: transition.getOut()) {
			Node node = arc.getTarget();
			if (node instanceof Place) {
				Place place = (Place) node;
				Token token = PetrinetFactory.eINSTANCE.createToken();
				this.append(new CreateChildCommand(domain, place, PetrinetPackage.eINSTANCE.getPlace_Tokens(), token, null));
			}
		}
	}

}
