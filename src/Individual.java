// Individual.java

public class Individual {

    // genes: This is the brain of the snake. It's an array of doubles
    // that represents the weights and biases of a neural network.
    public double[] genes;

    // fitness: This is the snake's score. It's an integer that represents
    // how well the snake performed in a single game simulation.
    public int fitness;

    // A constructor to easily create a new Individual with a given gene size.
    public Individual(int geneSize) {
        this.genes = new double[geneSize];
    }

    public double[] getGenes() {
        return genes;
    }

    public void setFitness(int fitness) {
        this.fitness = fitness;
    }
}