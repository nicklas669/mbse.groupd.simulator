package dk.dtu.compute.se.mdsu.tutorial1.simulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.notation.View;

import dk.dtu.compute.se.mdsu.emf.command.FireTransitionCommand;
import dk.dtu.compute.se.mdsu.petrinet.Arc;
import dk.dtu.compute.se.mdsu.petrinet.Node;
import dk.dtu.compute.se.mdsu.petrinet.PetrinetFactory;
import dk.dtu.compute.se.mdsu.petrinet.Place;
import dk.dtu.compute.se.mdsu.petrinet.Token;
import dk.dtu.compute.se.mdsu.petrinet.Transition;

public class SimulatorCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Transition transition = getTransition(event.getApplicationContext());
		if (isEnabled(transition)) {
			
			// There are three different ways to do a change to a model, dependent on
			// on whether the object is associated with an editing domain and with
			// which kind of editing domain:
			EditingDomain domain = AdapterFactoryEditingDomain.getEditingDomainFor(transition);
			if (domain instanceof TransactionalEditingDomain) {
				// An easy way to implement commands (which record themselves and this way
				// support the undo/redo mechanism); but this in works in transactional editing
				// Domains only (e.g in GMF editors but not in EMF editors)
				domain.getCommandStack().execute(
						new RecordingCommand((TransactionalEditingDomain) domain, "Fire Transition (2)") {

							@Override
							protected void doExecute() {
								// method from Tutorial 1
								// The recording command records, whatever is done here, and this
								// way is able to undo it without "progamming" the command explicitly
								// from basic commands
								fire(transition);
							}

						});
			} else if (domain != null ) {
				// Tutorial 2: manually implemented EMF command executed on the command stack;
				// (works in any editing domain)
				domain.getCommandStack().execute(new FireTransitionCommand(domain, transition));
			} else {
				// Tutorial 1:
				// fire directly, if transition is not associated with an editing domain;
				// in objects associated with normal editing domains this would work too,
				// but it would mess up with the undo/redo mechanism; in transactional
				// editing domains this would not work at all -- cause an exception (but 
				// we can encapsulate the call in an RecordingCommand when used with a 
				// transactional editing domain (see first case).
				fire(transition);
			}
		}
		return null;
	}

	@Override
	public void setEnabled(Object context) {
		Transition transition = getTransition(context);
		setBaseEnabled(isEnabled(transition));
	}
	
	static private Transition getTransition(Object context) {
		if (context instanceof IEvaluationContext) {
			IEvaluationContext evaluationContext = (IEvaluationContext) context;
			Object object = evaluationContext.getDefaultVariable();
			if (object instanceof List) {
				@SuppressWarnings("rawtypes")
				List list = (List) object;
				if (list.size() == 1) {
					object = list.get(0);
					if (object instanceof Transition) {
						return (Transition) object;
					} else if (object instanceof IGraphicalEditPart) {
						IGraphicalEditPart editPart = (IGraphicalEditPart) object;
						Object model = editPart.getModel();
						if (model instanceof View) {
							Object element = ((View) model).getElement();
							if (element instanceof Transition) {
								return (Transition) element;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	static private boolean isEnabled(Transition transition) {
		if (transition != null) {
			// compute the number of tokens needed for each place in the map needed;
			// this is necessary because some places might occur twice in the preset
			// of a transition
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
			
			// check whether each place has the number of needed tokens
			for (Place place: needed.keySet()) {
				if (place.getTokens().size() < needed.get(place)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	static private void fire(Transition transition) {
		if (transition != null) {
			for (Arc arc: transition.getIn()) {
				Node node = arc.getSource();
				if (node instanceof Place) {
					Place place = (Place) node;
					List<Token> tokens = place.getTokens();
					if (!tokens.isEmpty()) {
						place.getTokens().remove(0);
					}
				}
			}
			
			for (Arc arc: transition.getOut()) {
				Node node = arc.getTarget();
				if (node instanceof Place) {
					Place place = (Place) node;
					Token token = PetrinetFactory.eINSTANCE.createToken();
					place.getTokens().add(token);
				}
			}
		}
	}
	
}
