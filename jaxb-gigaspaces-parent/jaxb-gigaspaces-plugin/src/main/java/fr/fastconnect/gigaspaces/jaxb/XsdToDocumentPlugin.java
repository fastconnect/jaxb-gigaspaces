package fr.fastconnect.gigaspaces.jaxb;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.gigaspaces.document.SpaceDocument;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JAnnotationValue;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

public class XsdToDocumentPlugin extends Plugin {
	private static final JType[] NONE = new JType[0];

	@Override
	public String getOptionName() {
		return "XopenspacesDocument";
	}

	@Override
	public String getUsage() {
		return "-XopenspacesDocument  :  Create the classes as space document.";
	}

	@Override
	public boolean run(Outline model, Options opt, ErrorHandler errorHandler) throws SAXException {
		for (ClassOutline co : model.getClasses()) {
			processClassOutline(co);
		}
		return true;
	}

	protected void processClassOutline(ClassOutline classOutline) throws SAXException {
		final JDefinedClass implClass = classOutline.implClass;

		// add Space Document extention
		if (implClass._extends().fullName().equals(Object.class.getName())) {
			implClass._extends(SpaceDocument.class);
		} else {
			throw new SAXException("inherated classes are not supported yet with SpaceDocuments parsing.");
		}
		// remove every field and replace it by a static String definition
		generateSetters(classOutline, implClass);
	}

	@SuppressWarnings("unchecked")
	private void generateSetters(ClassOutline classOutline, JDefinedClass implClass) {
		Map<String, JFieldVar> fields = implClass.fields();
		// copy the current field map into another one that is not linked to the implClass.
		Map<String, JFieldVar> originalFields = new HashMap<String, JFieldVar>(fields);

		FieldOutline[] fieldOutlines = classOutline.getDeclaredFields();

		for (final FieldOutline fieldOutline : fieldOutlines) {
			// for (final Entry<String, JFieldVar> entry : originalFields.entrySet()) {
			// remove field from the class...
			JFieldVar fieldVar = originalFields.get(fieldOutline.getPropertyInfo().getName(false));
			classOutline.implClass.removeField(fieldVar);
			final String fieldName = fieldVar.name();

			System.out.println("processing " + implClass.name() + " field " + fieldVar.name());

			// TODO generate camel-case static name
			// generate a static String variable which will be the Document field name
			implClass.field(JMod.PRIVATE + JMod.FINAL + JMod.STATIC, String.class, fieldName.toUpperCase(),
					new JExpressionImpl() {
						public void generate(JFormatter paramJFormatter) {
							paramJFormatter.p(JExpr.quotify('"', fieldName));
						}
					});

			final String publicName = fieldOutline.getPropertyInfo().getName(true);

			final String getterName = "get" + publicName;
			final JMethod getter = implClass.getMethod(getterName, NONE);

			try {
				// we have to place the field annotations generated by JAXB on the methods
				Field field = JVar.class.getDeclaredField("annotations");
				field.setAccessible(true);
				// find all the annotations
				List<JAnnotationUse> annotations = (List<JAnnotationUse>) field.get(fieldVar);
				if (annotations != null) {
					for (JAnnotationUse annotationUse : annotations) {
						// get the annotation class
						Field aClassField = JAnnotationUse.class.getDeclaredField("clazz");
						aClassField.setAccessible(true);
						JClass clazz = (JClass) aClassField.get(annotationUse);
						// get the parameters
						Field aParamField = JAnnotationUse.class.getDeclaredField("memberValues");
						aParamField.setAccessible(true);
						Map<String, JAnnotationValue> memberValues = (Map<String, JAnnotationValue>) aParamField
								.get(annotationUse);
						// add annotation to the related getter
						JAnnotationUse methodAnnotation = getter.annotate(clazz);
						aParamField.set(methodAnnotation, memberValues);
					}
				}
				//
				// // we want to replace the classical body so we have to reset it to null
				Field bodyField = JMethod.class.getDeclaredField("body");
				bodyField.setAccessible(true);
				bodyField.set(getter, null);

				final JType type = getter.type();
				if (type.isPrimitive()) {
					getter.body()._return(
							JExpr.cast(
									type.boxify(),
									JExpr._this().invoke("getProperty")
											.arg(JExpr.direct((implClass.name() + "." + fieldName.toUpperCase())))));
				} else {
					getter.body()._return(
							JExpr._this().invoke("getProperty")
									.arg(JExpr.direct((implClass.name() + "." + fieldName.toUpperCase()))));
				}

				final String setterName = "set" + publicName;
				final JMethod boxifiedSetter = implClass.getMethod(setterName, new JType[] { type.boxify() });
				final JMethod unboxifiedSetter = implClass.getMethod(setterName, new JType[] { type.unboxify() });
				final JMethod setter = boxifiedSetter != null ? boxifiedSetter : unboxifiedSetter;
				// better keep unboxified....
				if (setter != null) {
					bodyField.set(setter, null);
					setter.body();
					setter.body().invoke("setProperty")
							.arg(JExpr.direct((implClass.name() + "." + fieldName.toUpperCase())))
							.arg(setter.listParams()[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}