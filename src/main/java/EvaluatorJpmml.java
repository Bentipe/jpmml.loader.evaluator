import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.TargetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvaluatorJpmml {

    private Evaluator evaluator;

    private static final Logger logger = LoggerFactory.getLogger(EvaluatorJpmml.class);

    public EvaluatorJpmml(String path) {

        this.evaluator = load(path);

    }

    public Double predict(HashMap<String, Double> pfeatures) {

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        List<InputField> inputFields = evaluator.getInputFields();

        if (pfeatures.size() != inputFields.size())
            throw new IllegalArgumentException("ERROR Size of given features is not the same as the input required!");

        for (int i = 0; i < inputFields.size(); i++) {
            InputField inputField = inputFields.get(i);
            String inputFieldname = inputField.getMiningField().getName().getValue();
            Object raw = pfeatures.get(inputFieldname);
            FieldValue inputFieldValue = inputField.prepare(raw);
            arguments.put(inputField.getName(), inputFieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);

        Object targetFieldValue = null;

        List<TargetField> targetFields = evaluator.getTargetFields();


        for (TargetField targetField : targetFields) {
            FieldName targetFieldName = targetField.getName();
            targetFieldValue = results.get(targetFieldName);
        }

        Object unboxedTargetFieldValue = null;


        if (targetFieldValue instanceof Computable) {
            Computable computable = (Computable) targetFieldValue;

            unboxedTargetFieldValue = computable.getResult();
        }

        if (null == unboxedTargetFieldValue) {
            return 0D;
        } else {
            return (Double) unboxedTargetFieldValue;
        }

    }

    private Evaluator load(String path) {
        PMML pmml;
        File file = new File(path);
        try {
            InputStream is = new FileInputStream(file);
            pmml = org.jpmml.model.PMMLUtil.unmarshal(is);
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            return modelEvaluatorFactory.newModelEvaluator(pmml);
        } catch (FileNotFoundException | SAXException | JAXBException e) {
            e.printStackTrace();
        }

        return null;
    }

}
