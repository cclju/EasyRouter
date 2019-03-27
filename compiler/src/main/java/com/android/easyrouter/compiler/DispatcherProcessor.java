package com.android.easyrouter.compiler;

import com.android.easyrouter.annotation.DisPatcher;
import com.android.easyrouter.annotation.DispatcherModules;
import com.android.easyrouter.annotation.ModuleService;
import com.android.easyrouter.compiler.constant.CompilerConstant;
import com.android.easyrouter.compiler.exception.InvalidTargetException;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static com.squareup.javapoet.JavaFile.builder;


/**
 * APT
 */
@SupportedOptions(CompilerConstant.KEY_MODULE_NAME)
@AutoService(Processor.class)
public class DispatcherProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Filer mFiler;
    private Elements elementUtils;
    private String moduleName = "";
    private Set<String> set;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        set = new HashSet<String>();

        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(CompilerConstant.KEY_MODULE_NAME);
            System.out.println("DispatcherProcessor init moduleName = " + moduleName);
            set.add(moduleName);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(DisPatcher.class.getCanonicalName());
        ret.add(DispatcherModules.class.getCanonicalName());
        ret.add(ModuleService.class.getCanonicalName());
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> modulesList = roundEnv.getElementsAnnotatedWith(DispatcherModules.class);

        Set<? extends Element> elementDispatchers = roundEnv.getElementsAnnotatedWith(DisPatcher.class);
        Set<? extends Element> elementModuleServices = roundEnv.getElementsAnnotatedWith(ModuleService.class);

        String[] moduleNames = null;
        if (modulesList != null && modulesList.size() > 0) {
            Element modules = modulesList.iterator().next();
            moduleNames = modules.getAnnotation(DispatcherModules.class).value();
        }
        try {
            TypeSpec type = getRouterTableInitializer(elementDispatchers, elementModuleServices);
            if (type != null) {
                // e.g. build/generated/source/apt/debug/com/android/easyrouter/AutoCreateModuleActivityMap_{moduleName} implements IActivityInitMap
                builder(CompilerConstant.AutoCreateDispatcherPackage, type).build().writeTo(mFiler);
            }
            if (moduleNames != null && moduleNames.length > 0) {
                TypeSpec typeInit = generateModulesRouterInit(moduleNames);
                if (typeInit != null) {
                    // e.g. com.android.easyrouter.RouterInit
                    builder(CompilerConstant.AutoCreateDispatcherPackage, typeInit).build().writeTo(mFiler);
                }
            }
        } catch (FilerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            error(e.getMessage());
        }
        return true;
    }


    private TypeSpec generateModulesRouterInit(String[] moduleNames) {
        MethodSpec.Builder initActivityDispatcherMethod = MethodSpec.methodBuilder("initActivityDispatcher")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        for (String module : moduleNames) {
            initActivityDispatcherMethod.addStatement("com.android.easyrouter.dispatcher.dispatcherimpl.ActivityDispatcher.getActivityDispatcher().initActivityMaps(new " +
                    CompilerConstant.AutoCreateActivityMapPrefix + module + "())");
        }

        MethodSpec.Builder initModuleServiceMethod = MethodSpec.methodBuilder("initModuleService")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        for (String module : moduleNames) {
            initModuleServiceMethod.addStatement(CompilerConstant.AutoCreateActivityMapPrefix + module + ".initModuleService()");
        }

        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        initMethod.addStatement("initActivityDispatcher()")
                .addStatement("initModuleService()");

        return TypeSpec.classBuilder("RouterInit")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(initActivityDispatcherMethod.build())
                .addMethod(initModuleServiceMethod.build())
                .addMethod(initMethod.build())
                .build();
    }


    private TypeSpec getRouterTableInitializer(Set<? extends Element> elements, Set<? extends Element> moduleServiceElements) throws ClassNotFoundException {
        if (elements == null || elements.size() == 0) {
            return null;
        }
        ParameterizedTypeName mapTypeName = ParameterizedTypeName
                .get(ClassName.get(HashMap.class), ClassName.get(String.class),
                        ClassName.get(Class.class));
        ParameterSpec mapParameterSpec = ParameterSpec.builder(mapTypeName, "activityMap")
                .build();

        // method = initActivityMap
        MethodSpec.Builder routerInitBuilder = MethodSpec.methodBuilder("initActivityMap")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mapParameterSpec);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                throw new InvalidTargetException("DisPatcher must be used on class");
            }
            DisPatcher router = element.getAnnotation(DisPatcher.class);
            String[] routerUrls = router.value();
            for (String routerUrl : routerUrls) {
                routerInitBuilder.addStatement("activityMap.put($S, $T.class)", routerUrl, ClassName.get((TypeElement) element));
            }
        }

        // method = initModuleService
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("initModuleService")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        for (Element element : moduleServiceElements) {
            initMethod.addStatement("com.android.easyrouter.service.ModuleServiceManager.register(com.android.easyrouter.service.BaseModuleService.$T.class, new $T()) ",
                    ClassName.get((TypeElement) element), ClassName.get((TypeElement) element));
        }

        TypeElement routerInitializerType = elementUtils.getTypeElement("com.android.easyrouter.dispatcher.idispatcher.IActivityInitMap");
        return TypeSpec.classBuilder(CompilerConstant.AutoCreateActivityMapPrefix + moduleName)
                .addSuperinterface(ClassName.get(routerInitializerType))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(routerInitBuilder.build())
                .addMethod(initMethod.build())
                .build();
    }


    private void error(String error) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, error);
    }


}
