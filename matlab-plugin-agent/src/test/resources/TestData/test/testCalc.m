classdef testCalc < matlab.unittest.TestCase
    %TESTCALC Summary of this class goes here
    %   Detailed explanation goes here
    
    methods(Test)
        function testSquare(testCase)
            %TESTCALC Construct an instance of this class
            %   Detailed explanation goes here
            testCase.verifyEqual(sqNum(2), 4);
        end
        
        function testMultiplication(testCase)
            %METHOD1 Summary of this method goes here
            %   Detailed explanation goes here
            testCase.verifyEqual(mulNum(2,2), 4);
        end
    end
end

