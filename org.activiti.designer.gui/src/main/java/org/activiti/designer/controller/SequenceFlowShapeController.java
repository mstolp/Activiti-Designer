package org.activiti.designer.controller;

import java.util.List;

import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.Gateway;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.Task;
import org.activiti.designer.diagram.ActivitiBPMNFeatureProvider;
import org.activiti.designer.util.TextUtil;
import org.activiti.designer.util.editor.BpmnMemoryModel;
import org.activiti.designer.util.editor.ModelHandler;
import org.activiti.designer.util.platform.OSEnum;
import org.activiti.designer.util.platform.OSUtil;
import org.activiti.designer.util.style.StyleUtil;
import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.MultiText;
import org.eclipse.graphiti.mm.algorithms.Polygon;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.algorithms.styles.StylesFactory;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.ChopboxAnchor;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.IColorConstant;

/**
 * A {@link BusinessObjectShapeController} capable of creating and updating shapes for {@link Task} objects.
 *  
 * @author Tijs Rademakers
 */
public class SequenceFlowShapeController extends AbstractBusinessObjectShapeController {
  
  public SequenceFlowShapeController(ActivitiBPMNFeatureProvider featureProvider) {
    super(featureProvider);
  }

  @Override
  public boolean canControlShapeFor(Object businessObject) {
    if (businessObject instanceof SequenceFlow) {
      return true;
    } else {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public PictogramElement createShape(Object businessObject, ContainerShape layoutParent, int width, int height, IAddContext context) {
    final IPeCreateService peCreateService = Graphiti.getPeCreateService();
    final IGaService gaService = Graphiti.getGaService();
    
    Diagram diagram = getFeatureProvider().getDiagramTypeProvider().getDiagram();
    
    IAddConnectionContext addConContext = (IAddConnectionContext) context;
    SequenceFlow addedSequenceFlow = (SequenceFlow) context.getNewObject();
    
    Anchor sourceAnchor = null;
    Anchor targetAnchor = null;
    if(addConContext.getSourceAnchor() == null) {
      EList<Shape> shapeList = diagram.getChildren();
      for (Shape shape : shapeList) {
        FlowNode flowNode = (FlowNode) getFeatureProvider().getBusinessObjectForPictogramElement(
                shape.getGraphicsAlgorithm().getPictogramElement());
        if(flowNode == null || flowNode.getId() == null || addedSequenceFlow.getSourceRef() == null ||
                addedSequenceFlow.getTargetRef() == null) continue;
        if(flowNode.getId().equals(addedSequenceFlow.getSourceRef())) {
          EList<Anchor> anchorList = ((ContainerShape) shape).getAnchors();
          for (Anchor anchor : anchorList) {
            if(anchor instanceof ChopboxAnchor) {
              sourceAnchor = anchor;
              break;
            }
          }
        }
        
        if(flowNode.getId().equals(addedSequenceFlow.getTargetRef())) {
          EList<Anchor> anchorList = ((ContainerShape) shape).getAnchors();
          for (Anchor anchor : anchorList) {
            if(anchor instanceof ChopboxAnchor) {
              targetAnchor = anchor;
              break;
            }
          }
        }
      }
    } else {
      sourceAnchor = addConContext.getSourceAnchor();
      targetAnchor = addConContext.getTargetAnchor();
    }
    
    if(sourceAnchor == null || targetAnchor == null) {
      return null;
    }

    // CONNECTION WITH POLYLINE
    FreeFormConnection connection = peCreateService.createFreeFormConnection(diagram);
    connection.setStart(sourceAnchor);
    connection.setEnd(targetAnchor);
    sourceAnchor.getOutgoingConnections().add(connection);
    targetAnchor.getIncomingConnections().add(connection);

    BpmnMemoryModel model = ModelHandler.getModel(EcoreUtil.getURI(diagram));
    
    FlowElement sourceElement = model.getFlowElement(addedSequenceFlow.getSourceRef());
    FlowElement targetElement = model.getFlowElement(addedSequenceFlow.getTargetRef());
    
    GraphicsAlgorithm sourceGraphics = getPictogramElement(sourceElement).getGraphicsAlgorithm();
    GraphicsAlgorithm targetGraphics = getPictogramElement(targetElement).getGraphicsAlgorithm();
    
    List<GraphicInfo> bendpointList = null;
    if(addConContext.getProperty("org.activiti.designer.bendpoints") != null) {
      bendpointList = (List<GraphicInfo>) addConContext.getProperty("org.activiti.designer.bendpoints");
    }
    
    if(bendpointList != null && bendpointList.size() >= 0) {
      for (GraphicInfo graphicInfo : bendpointList) {
        Point bendPoint = StylesFactory.eINSTANCE.createPoint();
        bendPoint.setX((int)graphicInfo.getX());
        bendPoint.setY((int)graphicInfo.getY());
        connection.getBendpoints().add(bendPoint);
      }
      
    } else {
      
      Shape sourceShape = (Shape) getPictogramElement(sourceElement);
      ILocation sourceShapeLocation = Graphiti.getLayoutService().getLocationRelativeToDiagram(sourceShape);
      int sourceX = sourceShapeLocation.getX();
      int sourceY = sourceShapeLocation.getY();
      
      Shape targetShape = (Shape) getPictogramElement(targetElement);
      ILocation targetShapeLocation = Graphiti.getLayoutService().getLocationRelativeToDiagram(targetShape);
      int targetX = targetShapeLocation.getX();
      int targetY = targetShapeLocation.getY();
      
      if (sourceElement instanceof Gateway && targetElement instanceof Gateway == false) {
        if (((sourceGraphics.getY() + 10) < targetGraphics.getY()
            || (sourceGraphics.getY() - 10) > targetGraphics.getY())  && 
            (sourceGraphics.getX() + (sourceGraphics.getWidth() / 2)) < targetGraphics.getX()) {
          
          boolean subProcessWithBendPoint = false;
          if(targetElement instanceof SubProcess) {
            int middleSub = targetGraphics.getY() + (targetGraphics.getHeight() / 2);
            if((sourceGraphics.getY() + 20) < middleSub || (sourceGraphics.getY() - 20) > middleSub) {
              subProcessWithBendPoint = true;
            }
          }
          
          if(targetElement instanceof SubProcess == false || subProcessWithBendPoint == true) {
            Point bendPoint = StylesFactory.eINSTANCE.createPoint();
            bendPoint.setX(sourceX + 20);
            bendPoint.setY(targetY + (targetGraphics.getHeight() / 2));
            connection.getBendpoints().add(bendPoint);
          }
        }
      } else if (targetElement instanceof Gateway) {
        if (((sourceGraphics.getY() + 10) < targetGraphics.getY()
            || (sourceGraphics.getY() - 10) > targetGraphics.getY()) && 
            (sourceGraphics.getX() + sourceGraphics.getWidth()) < targetGraphics.getX()) {
          
          boolean subProcessWithBendPoint = false;
          if(sourceElement instanceof SubProcess) {
            int middleSub = sourceGraphics.getY() + (sourceGraphics.getHeight() / 2);
            if((middleSub + 20) < targetGraphics.getY() || (middleSub - 20) > targetGraphics.getY()) {
              subProcessWithBendPoint = true;
            }
          }
          
          if(sourceElement instanceof SubProcess == false || subProcessWithBendPoint == true) {
            Point bendPoint = StylesFactory.eINSTANCE.createPoint();
            bendPoint.setX(targetX + 20);
            bendPoint.setY(sourceY + (sourceGraphics.getHeight() / 2));
            connection.getBendpoints().add(bendPoint);
          }
        }
      } else if (targetElement instanceof EndEvent) {
        int middleSource = sourceGraphics.getY() + (sourceGraphics.getHeight() / 2);
        int middleTarget = targetGraphics.getY() + (targetGraphics.getHeight() / 2);
        if (((middleSource + 10) < middleTarget && 
            (sourceGraphics.getX() + sourceGraphics.getWidth()) < targetGraphics.getX()) ||
            
            ((middleSource - 10) > middleTarget && 
            (sourceGraphics.getX() + sourceGraphics.getWidth()) < targetGraphics.getX())) {
          
          Point bendPoint = StylesFactory.eINSTANCE.createPoint();
          bendPoint.setX(targetX + (targetGraphics.getWidth() / 2));
          bendPoint.setY(sourceY + (sourceGraphics.getHeight() / 2));
          connection.getBendpoints().add(bendPoint);
        }
      }
    }

    Polyline polyline = gaService.createPolyline(connection);
    polyline.setLineStyle(LineStyle.SOLID);
    polyline.setForeground(Graphiti.getGaService().manageColor(diagram, IColorConstant.BLACK));

    // add dynamic text decorator for the reference name
    ConnectionDecorator textDecorator = peCreateService.createConnectionDecorator(connection, true, 0.5, true);
    MultiText text = gaService.createDefaultMultiText(diagram, textDecorator);
    text.setStyle(StyleUtil.getStyleForTask((diagram)));
    text.setHorizontalAlignment(Orientation.ALIGNMENT_LEFT);
    text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
    if (OSUtil.getOperatingSystem() == OSEnum.Mac) {
      text.setFont(gaService.manageFont(diagram, text.getFont().getName(), 11));
    }
    
    // set reference name in the text decorator
    text.setValue(addedSequenceFlow.getName());
    
    if(addConContext.getProperty("org.activiti.designer.connectionlabel") != null) {
      GraphicInfo labelLocation = (GraphicInfo) addConContext.getProperty("org.activiti.designer.connectionlabel");
      gaService.setLocation(text, (int)labelLocation.getX(), (int)labelLocation.getY());
      if (StringUtils.isNotEmpty(addedSequenceFlow.getName())) {
        TextUtil.setTextSize((int) labelLocation.getWidth(), text);
      }
    } else {
      gaService.setLocation(text, 10, 0);
      if (StringUtils.isNotEmpty(addedSequenceFlow.getName())) {
        TextUtil.setTextSize(text);
      }
    }

    // add static graphical decorators (composition and navigable)
    ConnectionDecorator cd = peCreateService.createConnectionDecorator(connection, false, 1.0, true);
    createArrow(cd, diagram);

    return connection;
  }
    
  protected Polygon createArrow(GraphicsAlgorithmContainer gaContainer, Diagram diagram) {
    int xy[] = new int[] { -10, -5, 0, 0, -10, 5, -8, 0 };
    int beforeAfter[] = new int[] { 3, 3, 0, 0, 3, 3, 3, 3 };
    Polygon polyline = Graphiti.getGaCreateService().createPolygon(gaContainer, xy, beforeAfter);
    polyline.setStyle(StyleUtil.getStyleForPolygon(diagram));
    return polyline;
  }

  protected PictogramElement getPictogramElement(Object businessObject) {
    return getFeatureProvider().getPictogramElementForBusinessObject(businessObject);
  }
}
