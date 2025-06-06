package simulation;

import com.jgoodies.forms.factories.Borders;

import repast.simphony.context.Context;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.grid.*;

public class MinimalContext implements ContextBuilder<Object> {
    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("Simulation");

        GridFactory gridFactory = GridFactoryFinder . createGridFactory ( null );
         Grid < Object > grid = gridFactory . createGrid ( "gabisi" , context ,
         new GridBuilderParameters < Object >( new WrapAroundBorders () ,
         new SimpleGridAdder < Object >() ,
         true , 50 , 50));

        MinimalAgent agent = new MinimalAgent();
        context.add(agent);
        grid.moveTo(agent, 2, 2);

        RunEnvironment.getInstance().endAt(10);
        return context;
    }
}
