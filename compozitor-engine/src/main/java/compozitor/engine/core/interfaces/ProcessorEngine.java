package compozitor.engine.core.interfaces;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.common.io.CharStreams;

import compozitor.generator.core.interfaces.GeneratedCode;
import compozitor.processor.core.interfaces.AnnotationProcessor;
import compozitor.template.core.interfaces.Template;
import compozitor.template.core.interfaces.TemplateEngine;
import compozitor.template.core.interfaces.TemplateEngineBuilder;

public abstract class ProcessorEngine<T> extends AnnotationProcessor {
	private final CodeEngine<T> engine;
	private final EngineContext<T> context;
	private final TemplateEngine templateEngine;
	private StateHandler stateHandler;

	public ProcessorEngine() {
		this.engine = CodeEngine.create();
		this.context = EngineContext.create();
		this.templateEngine = this.init(TemplateEngineBuilder.create().withClasspathTemplateLoader());
		this.stateHandler = ((ise) -> { throw new RuntimeException(ise);});
	}

	protected TemplateEngine init(TemplateEngineBuilder builder) {
		return builder.build();
	}

	@Override
	protected final void postProcess() {
		this.load(this.context);

		this.engine.generate(context, code -> {
			this.write(code);
		});
	}

	private void write(GeneratedCode code) {
		Filer filer = this.processingEnv.getFiler();
		
		try {
			String sourceCode = CharStreams.toString(new InputStreamReader(code.getContent()));
			
			System.out.println(sourceCode);
			
			FileObject sourceFile = filer.getResource(StandardLocation.SOURCE_OUTPUT, code.getNamespace(), code.getSimpleName());
			
			try(Writer writer = sourceFile.openWriter()){
				writer.write(sourceCode);
			}
		}catch (IOException e) {
			throw new RuntimeException(e);
		}catch (IllegalStateException ise) {
			this.stateHandler.accept(ise);
		}
	}
	
	public void setHandler(StateHandler handler) {
		this.stateHandler = handler;
	}

	protected final Template getTemplate(String resourceName) {
		return this.templateEngine.getTemplate(resourceName);
	}

	protected abstract void load(EngineContext<T> context);
}