package com.matteominin.pdf_extractor.util;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.List;

public class ImageAwareTextStripper extends PDFTextStripper {

    public ImageAwareTextStripper() throws IOException {
        super();
        // Set this to true to ensure text is processed in the correct visual order
        this.setSortByPosition(true);
    }

    /**
     * Overrides the default processOperator to handle the 'Do' operator for images.
     * This method is called by PDFBox for every operator in the content stream.
     */
    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        // Check for the 'Do' operator, which is used to draw an external object (like an image)
        if (OperatorName.DRAW_OBJECT.equals(operator.getName())) {
            // The first operand is the name of the object to be drawn
            COSName objectName = (COSName) operands.get(0);
            PDResources resources = getResources();

            // Look up the object in the page's resources
            PDXObject xObject = resources.getXObject(objectName);

            // If the object is an image, we handle it
            if (xObject instanceof PDImageXObject) {
                // Get the current text output stream and append our placeholder
                // A newline is added for better readability
                getOutput().write("\n[IMAGE GOES HERE]\n");
            }
        }
        
        // Pass the operator to the superclass to ensure all other operations (like text) are handled as normal
        super.processOperator(operator, operands);
    }

    /**
     * Overrides the default writeString method to capture text positions, which can be used to
     * determine the correct location for image placeholders.
     */
    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        // This is where PDFTextStripper writes the text to the output stream.
        // We can add logic here if we need to be more precise with placeholder placement
        // based on the Y-coordinate of the text line.
        
        // For a simple solution, we just let the superclass handle writing the text.
        super.writeString(text, textPositions);
    }
}