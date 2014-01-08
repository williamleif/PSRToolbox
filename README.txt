PSRToolbox
==========
Author: William L Hamilton, McGill University, Reasoning and Learning Lab
Contact: william.hamilton2@mail.mcgill.ca

Collection of methods and tools for using PSRs to model dynamical systems and to construct model-based reinforcement learning algorithms. 
The PSR code optionally uses randomized compression techniques to increase performance (so called "Compressed Predictive State Representations"). 
For details see the README or refer to the paper "Efficient Learning and Planning with Compressed Predictive States" at arXiv:1312.0286 [cs.LG].
The provided Java documentation should also be of aid. 

NOTE: Currently I am shifting most of my code to other languages (e.g., C++ for speed, Python for usability).
Thus I will not be regularly committing to this repo. If you have specific questions, please email me. 

DISCLAIMER: The code is a both fully functional and a work in progress (as is most research code). As such, I cannot guarantee particular functionality.
Moreover, the code is not intended for commerical use. Also, it is not intended as an out-of-the-box package and users will likely need to write code to create their required functionality. The code is, in essence, a large library of methods and classes. 

ACKNOWLEDGMENTS: Much thanks to Guillame Saulnier for providing his Java random forests code.

USING THE CODE:

Before running anything the libraries in the lib directory must be added to the Java classpath.

The PSRToolbox contains a large set of related Java classes for using PSR-based models for prediction or creating reinforcement learning agents.
There are many possible use scenarios and the code is not intended as an out-of-the-box package.
Thus, no simple concrete steps for use can be provided; a general overview of the code structure in the context of a (somewhat) typical use case is provided below.

For reinforcement learning applications, one would make use of the PSRPlanningExperiment class.
This class contains high level methods that connect a PSR model, to a simulator.

The planning aspects of the code are contained within the "planning" java package.
This code using a version of fittedQ, which in turn uses random forests.

The PSR models are contained within the model section.
The different models all offer different tradeoffs.
TPSR is the a standard PSR model, CPSR uses random projections for compression, etc.

I encourage potential users to contact me directly with specific use cases (as the code-base is quite large), or to refer to the paper cited above.


