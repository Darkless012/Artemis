/* ChadoTransactionManager.java
 *
 * created: July 2005
 *
 * This file is part of Artemis
 *
 * Copyright (C) 2005  Genome Research Limited
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package uk.ac.sanger.artemis.chado;

import uk.ac.sanger.artemis.Feature;
import uk.ac.sanger.artemis.FeatureSegment;
import uk.ac.sanger.artemis.FeatureSegmentVector;
import uk.ac.sanger.artemis.sequence.SequenceChangeListener;
import uk.ac.sanger.artemis.sequence.SequenceChangeEvent;
import uk.ac.sanger.artemis.components.genebuilder.ortholog.MatchPanel;
import uk.ac.sanger.artemis.components.genebuilder.ortholog.OrthoParalogTable;
import uk.ac.sanger.artemis.components.genebuilder.ortholog.SimilarityTable;
import uk.ac.sanger.artemis.io.DatabaseDocumentEntry;
import uk.ac.sanger.artemis.io.DocumentEntry;
import uk.ac.sanger.artemis.io.QualifierLazyLoading;
import uk.ac.sanger.artemis.io.QualifierVector;
import uk.ac.sanger.artemis.io.Qualifier;
import uk.ac.sanger.artemis.io.RangeVector;
import uk.ac.sanger.artemis.io.StreamQualifier;
import uk.ac.sanger.artemis.io.GFFStreamFeature;
import uk.ac.sanger.artemis.io.Range;
import uk.ac.sanger.artemis.io.InvalidRelationException;
import uk.ac.sanger.artemis.io.EntryInformationException;
import uk.ac.sanger.artemis.io.Key;
import uk.ac.sanger.artemis.io.ChadoCanonicalGene;
import uk.ac.sanger.artemis.util.StringVector;
import uk.ac.sanger.artemis.util.DatabaseDocument;
import uk.ac.sanger.artemis.util.ReadOnlyException;
import uk.ac.sanger.artemis.EntryGroup;
import uk.ac.sanger.artemis.FeatureChangeListener;
import uk.ac.sanger.artemis.FeatureChangeEvent;
import uk.ac.sanger.artemis.EntryChangeListener;
import uk.ac.sanger.artemis.EntryChangeEvent;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;
import java.util.List;
import java.util.Enumeration;
import javax.swing.JOptionPane;

import org.gmod.schema.sequence.FeatureCvTermProp;
import org.gmod.schema.sequence.FeatureCvTermPub;
import org.gmod.schema.sequence.FeatureLoc;
import org.gmod.schema.sequence.FeatureProp;
import org.gmod.schema.sequence.FeatureDbXRef;
import org.gmod.schema.sequence.FeaturePub;
import org.gmod.schema.sequence.FeatureRelationship;
import org.gmod.schema.sequence.FeatureSynonym;
import org.gmod.schema.sequence.FeatureCvTerm;
import org.gmod.schema.sequence.Synonym;

import org.gmod.schema.analysis.AnalysisFeature;
import org.gmod.schema.cv.*;
import org.gmod.schema.pub.Pub;
import org.gmod.schema.general.Db;
import org.gmod.schema.general.DbXRef;
import org.gmod.schema.sequence.FeatureCvTermDbXRef;

/**
 *
 * Chado transaction manager listens for feature, entry and sequence changes.
 * <code>ChadoTransactionManager</code> creates and tracks the feature insertions,
 * deletions, and updates to commit back to the database.
 *
 **/
public class ChadoTransactionManager
       implements FeatureChangeListener, EntryChangeListener, SequenceChangeListener 
{
  private static org.apache.log4j.Logger logger4j = 
    org.apache.log4j.Logger.getLogger(ChadoTransactionManager.class);
  
  public static boolean addSegments = true;
  private Vector sql = new Vector();
  
  /** GFF3 predefined tags, i.e. not feature_prop's */
  private static String reserved_tags[] = 
          {   "ID",
              "Name",
              "Alias",
              "Parent",
              "Target",
              "Gap",
              "Derives_from",
              "Dbxref",
              "Ontology_term",
              "score",
              "codon_start",
              MatchPanel.SIMILARITY,
              MatchPanel.ORTHOLOG,
              MatchPanel.PARALOG,
              "literature",
              "gff_source",      // program or database
              "gff_seqname" };   // seqID of coord system
           
  //controlled vocab tags
  public static String cv_tags[] =
            { "GO",
              "controlled_curation",
              "product",
              "class" };
  
  //synonym tags from cv
  private static String synonym_tags[] = null;
  private static String SYNONYM_TAG_CVNAME = "genedb_synonym_type";
  private EntryGroup entryGroup;

  public ChadoTransactionManager()
  {
    
  }
  
  public void setEntryGroup(final EntryGroup entryGroup)
  {
    this.entryGroup = entryGroup;
  }
  
  /**
   *  Invoked when a deletion or insertion occurs in a Bases object.
   **/
  public void sequenceChanged(final SequenceChangeEvent event)
  {
    if(event.getType() == SequenceChangeEvent.DELETION ||
       event.getType() == SequenceChangeEvent.INSERTION)
    {
      int start  = event.getPosition();
      int length = event.getSubSequence().length();
      
      //
      // update residues in srcfeature  
      DatabaseDocument doc = (DatabaseDocument)
         ((DocumentEntry)entryGroup.getSequenceEntry().getEMBLEntry()).getDocument();
      int newSequenceLength = entryGroup.getSequenceEntry().getEMBLEntry().getSequence().length();
      
      /*org.gmod.schema.sequence.Feature regionFeature = new org.gmod.schema.sequence.Feature();
      CvTerm cvTerm = new CvTerm();
      cvTerm.setName("region");
      regionFeature.setCvTerm(cvTerm);
      org.gmod.schema.sequence.Feature srcFeature = new org.gmod.schema.sequence.Feature();
      srcFeature.setFeatureId( Integer.parseInt(doc.getSrcFeatureId()) );
      srcFeature.setSeqLen(new Integer(
          entryGroup.getSequenceEntry().getEMBLEntry().getSequence().length()));
      FeatureLoc featureLoc = new FeatureLoc();
      featureLoc.setFeatureBySrcFeatureId(srcFeature);
      featureLoc.setFmin(new Integer(start-1));
      regionFeature.setFeatureLoc(featureLoc);
      regionFeature.setSeqLen(new Integer(length));
      
      if(event.getType() == SequenceChangeEvent.INSERTION)
      {
        regionFeature.setResidues(event.getSubSequence().getBytes());
        featureLoc.setFmax(new Integer(start));
      }
      else
      {
        featureLoc.setFmax(new Integer(start+length));
      }  
      ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.UPDATE, regionFeature, 
          null, null, null);*/
      
      FeatureForUpdatingResidues chadoFeature = new FeatureForUpdatingResidues();
      chadoFeature.setStartBase(start-1);
      chadoFeature.setLength(length);    

      if(event.getType() == SequenceChangeEvent.INSERTION)
      {
        chadoFeature.setNewSubSequence(event.getSubSequence());
        chadoFeature.setEndBase(start);
        chadoFeature.setBasesToEnd(newSequenceLength-event.getSubSequence().length()-start+1);
      }
      else
      {
        chadoFeature.setEndBase(start+length);
        chadoFeature.setBasesToEnd(newSequenceLength+event.getSubSequence().length()-start);
      }
      
      chadoFeature.setFeatureId( Integer.parseInt(doc.getSrcFeatureId()) );
      chadoFeature.setSeqLen(new Integer(
          entryGroup.getSequenceEntry().getEMBLEntry().getSequence().length()));
      
      ChadoTransaction tsn = 
        new ChadoTransaction(ChadoTransaction.UPDATE, chadoFeature, null, null, null);
      sql.add(tsn);
    }
    else
    {
      
    }
  }
  
  /**
   *  Implementation of the FeatureChangeListener interface.  We listen for
   *  changes in every feature of every entry in this group.
   **/ 
  public void featureChanged(FeatureChangeEvent event)
  {
    if(event.featureHasChanged())
    {
      if(!(event.getFeature().getEmblFeature() instanceof GFFStreamFeature))
        return;
      
      final GFFStreamFeature feature = 
        (GFFStreamFeature)event.getFeature().getEmblFeature();

      if(event.getType() == FeatureChangeEvent.SEGMENT_CHANGED)
      {
        RangeVector rv_new = event.getNewLocation().getRanges();
        RangeVector rv_old = event.getOldLocation().getRanges();
         
        logger4j.debug("SEGMENT_CHANGED "+rv_new.size()+"  "+rv_old.size());
        
        if(rv_old.size() > rv_new.size()) // segment deleted
        {
          logger4j.debug("SEGMENT_CHANGED DELETED");
          // delete segment
          int ideleted;
          Vector deleted = new Vector();
          boolean found;
          for(ideleted=0; ideleted<rv_old.size(); ideleted++)
          {   
            Range range = (Range)rv_old.get(ideleted);
            found = false;
            for(int j=0; j<rv_new.size(); j++)
            {
              if( ((Range)rv_new.get(j)).equals(range) )
                found = true;     
            }
            
            if(!found)
              deleted.add(new Integer(ideleted));
          }
   
          for(int i=0; i<deleted.size();i++)
          {
            ideleted = ((Integer)deleted.elementAt(i)).intValue();
            Range range_old = (Range)rv_old.elementAt(ideleted);
            String seg_id   = feature.getSegmentID(range_old);
            deleteFeature(seg_id, feature.getKey().getKeyString());
            feature.getSegmentRangeStore().remove(seg_id);
          }
          
          String new_id = feature.getSegmentID(rv_new);
          Qualifier qualifier = new Qualifier("ID", new_id);
          try
          {
            feature.setQualifier(qualifier);
          }
          catch(ReadOnlyException e)
          {
            e.printStackTrace();
          }
          catch(EntryInformationException e)
          {
            e.printStackTrace();
          }
          
          // update feature_relationship.rank
          processFeatureRelationshipRank(feature, rv_new, ChadoTransaction.UPDATE);
        }
        else if(rv_old.size() < rv_new.size()) // feature segment added
        {
          logger4j.debug("SEGMENT_CHANGED ADDED");

          if(addSegments)
          {
            FeatureSegmentVector segments = ((uk.ac.sanger.artemis.Feature) feature
                .getUserData()).getSegments();

            FeatureSegment segment;
            for(int iadd = 0; iadd < segments.size(); iadd++)
            {
              segment = segments.elementAt(iadd);
              Range range = segment.getRawRange();
              boolean found = false;
              for(int j = 0; j < rv_old.size(); j++)
              {
                if(((Range) rv_old.get(j)).equals(range))
                  found = true;
              }

              if(found)
                continue;

              String segment_uniquename = feature.getSegmentID(range);
              insertFeatureSegment(segment, segment_uniquename);
            }
          }
          
          processFeatureRelationshipRank(feature, rv_new, ChadoTransaction.UPDATE);
        }
      }
      else if(event.getType() == FeatureChangeEvent.LOCATION_CHANGED)
      {
        RangeVector rv_new = event.getNewLocation().getRanges();
        RangeVector rv_old = event.getOldLocation().getRanges();

        logger4j.debug("LOCATION_CHANGED "+feature.getFirstBase()+".."+feature.getLastBase()+
                              "   new="+rv_new.size()+" old="+rv_old.size());
        if(rv_new.size() != rv_old.size())
          return;
        
        ChadoTransaction tsn;
        int ichanged;
        Vector changes = new Vector();
        for(ichanged=0; ichanged<rv_old.size(); ichanged++)
        {
          Range rnew = (Range)rv_new.elementAt(ichanged);
          Range rold = (Range)rv_old.elementAt(ichanged);
   
          if(rnew.getStart() != rold.getStart() ||
             rnew.getEnd()   != rold.getEnd() ||
             (event.getOldLocation().isComplement(rold) !=
              event.getNewLocation().isComplement(rnew)))
            changes.add(new Integer(ichanged));
        }
 
        for(int i=0; i<changes.size();i++)
        {
          ichanged = ((Integer)changes.elementAt(i)).intValue();
          
          Range range_new = (Range)rv_new.elementAt(ichanged);
          Range range_old = (Range)rv_old.elementAt(ichanged);
          String seg_id   = feature.getSegmentID(range_new);
          
          if(seg_id == null)
            seg_id   = feature.getSegmentID(range_old);
          
          if(feature.getSegmentRangeStore() != null)
            feature.getSegmentRangeStore().put(seg_id, range_new);
          
          if(sql.size() > 0)
          {
            // collapse updating featureloc into one statement
            ChadoTransaction lastTsn = (ChadoTransaction)sql.lastElement();
            String thisKey = feature.getKey().getKeyString();
            if(thisKey.equals(DatabaseDocument.EXONMODEL))
              thisKey = "exon";
            if(lastTsn.getGff_feature() != null &&
               lastTsn.getType() == ChadoTransaction.UPDATE &&
               lastTsn.getFeatureKey() != null &&
               lastTsn.getFeatureKey().equals( thisKey ) &&
               lastTsn.getFeatureObject() instanceof FeatureLoc)
            {
              FeatureLoc floc = (FeatureLoc)lastTsn.getFeatureObject();
              if(floc.getFeatureByFeatureId().getUniqueName().equals(seg_id))
              {
                logger4j.debug("Removing last FeatureLoc ChadoTransaction");
                sql.remove(sql.size()-1);
              }
            }
          }
          
          FeatureLoc featureloc = getFeatureLoc(feature, seg_id, range_new);
          
          tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
                                     featureloc,
                                     feature.getLastModified(), feature,
                                     feature.getKey().getKeyString());

          sql.add(tsn);
        }
        
      }
      else if(event.getType() == FeatureChangeEvent.QUALIFIER_CHANGED)
      {
        logger4j.debug("QUALIFIER_CHANGED for "
            +event.getOldQualifiers().getQualifierByName("ID").getValues().get(0));
        
        editKeyAndQualifiers(event.getOldQualifiers(),event.getNewQualifiers(),
            event.getOldKey(), event.getNewKey(),
            feature, FeatureChangeEvent.QUALIFIER_CHANGED);
      }
      else if(event.getType() == FeatureChangeEvent.ALL_CHANGED)
      {
        logger4j.debug("ALL_CHANGED "+event.getOldKey().toString()+"  "+
                                          event.getNewKey().toString());
        
        editKeyAndQualifiers(event.getOldQualifiers(),event.getNewQualifiers(),
                             event.getOldKey(), event.getNewKey(),
                             feature, FeatureChangeEvent.ALL_CHANGED);
        
        if(event.getOldKey().compareTo( event.getNewKey() ) != 0 &&
           (event.getNewKey().toString().equals("gene") ||
            event.getNewKey().toString().equals("pseudogene")) &&
           feature.getChadoGene() == null)
        {
          ChadoCanonicalGene chado_gene = new ChadoCanonicalGene();
          chado_gene.setGene(feature);
          feature.setChadoGene(chado_gene);
        }
      }
    }
  }
 
  /**
   *  Invoked when an Entry is changed.
   **/
  public void entryChanged(EntryChangeEvent event)
  {
    if(event.getType() == EntryChangeEvent.FEATURE_ADDED)
    { 
      // if this is a duplicate feature then ignore
      if(event.isDuplicate())
      {
        Feature feature = event.getFeature();
        Qualifier qualifier_uniquename;
        try
        {
          qualifier_uniquename = feature.getQualifierByName("ID");
          logger4j.debug("FEATURE_ADDED ------> DUPLICATE "+
              (String)(qualifier_uniquename.getValues()).elementAt(0));
        }
        catch(InvalidRelationException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        return;
      }
     
      if(!(event.getFeature().getEmblFeature() instanceof GFFStreamFeature))
        return;
      
      final Feature feature = event.getFeature();
      
      final FeatureSegmentVector segments = feature.getSegments();
      
      if(segments != null && segments.size() > 1)
      {
        for(int iadd = 0; iadd < segments.size(); iadd++)
        {
          FeatureSegment segment = segments.elementAt(iadd);
          Range range = segment.getRawRange();
          final String segment_uniquename = 
            ((GFFStreamFeature)feature.getEmblFeature()).getSegmentID(range);
          insertFeatureSegment(segment, segment_uniquename);
        }
      }
      else
        insertFeature(feature);
    }
    else if(event.getType() == EntryChangeEvent.FEATURE_DELETED)
    { 
      if(event.isDuplicate())
      {
        logger4j.debug("FEATURE_DELETED looks like duplicate - ignore");
        return;
      }
      
      if(!(event.getFeature().getEntry().getEMBLEntry() instanceof DatabaseDocumentEntry) )
      {
        logger4j.debug("FEATURE_DELETED not a Database deletion");
        return;
      }
      
      try
      {
        Qualifier qualifier_uniquename = event.getFeature().getQualifierByName("ID");
        
        
        
        String feature_uniquename = 
                             (String)(qualifier_uniquename.getValues()).elementAt(0);
        
        GFFStreamFeature gff_feature =
          (GFFStreamFeature)event.getFeature().getEmblFeature();
        if(event.getFeature().getSegments().size() > 0)
        {
          RangeVector ranges = gff_feature.getLocation().getRanges();
          for(int i=0; i<ranges.size(); i++)
          {
            Range range = (Range)ranges.get(i);
            feature_uniquename = gff_feature.getSegmentID(range);
            deleteFeature(feature_uniquename, gff_feature.getKey().getKeyString());
          }    
        }
        else
          deleteFeature(feature_uniquename, gff_feature.getKey().getKeyString());
      }
      catch(InvalidRelationException e)
      {
        e.printStackTrace();
      }
    }

//  System.out.println(event.getEntry().getName());
  }
  
  /**
   * Update spliced features rank
   * @param feature
   * @param rv_new
   */ 
  private void processFeatureRelationshipRank(final GFFStreamFeature feature,
                                             final RangeVector rv_new,
                                             final int type)
  {   
    // update feature_relationship.rank
    ChadoTransaction tsn;
    Hashtable feature_relationship_rank_store = new Hashtable();
    Qualifier qualifier_relation = feature.getQualifierByName("Parent");
    
    for(int rank=0; rank<rv_new.size(); rank++)
    {
      Range range   = (Range)rv_new.elementAt(rank);
      String seq_id = feature.getSegmentID(range);
      
      org.gmod.schema.sequence.Feature chado_feature = 
               new org.gmod.schema.sequence.Feature();
      chado_feature.setUniqueName(seq_id);

      List featureRelationshipsForSubjectId = null;
      if(qualifier_relation != null)
      {
        StringVector parents = qualifier_relation.getValues();
        if(parents.size() > 0)
          featureRelationshipsForSubjectId = new Vector();
        
        for(int i=0; i<parents.size(); i++)
        {
          org.gmod.schema.sequence.Feature parent =
              new org.gmod.schema.sequence.Feature();
          parent.setUniqueName((String)parents.get(i));
          FeatureRelationship feature_relationship =
              new FeatureRelationship();
          
          //
          // should be retrieved from relationship ontology !!
          CvTerm cvterm = new CvTerm();
          cvterm.setCvTermId(DatabaseDocument.getCvtermID("part_of").intValue());
          
          feature_relationship.setFeatureByObjectId(parent);
          feature_relationship.setFeatureBySubjectId(chado_feature);
          feature_relationship.setCvTerm(cvterm);
          feature_relationship.setRank(rank);
          featureRelationshipsForSubjectId.add(feature_relationship);
          
          tsn = new ChadoTransaction(type,
              feature_relationship,
              feature.getLastModified(), feature,
              feature.getKey().getKeyString());
          sql.add(tsn);
        }
      }

      feature_relationship_rank_store.put(seq_id, new Integer(rank));
    }
    feature.setFeature_relationship_rank_store(feature_relationship_rank_store);  
  }
  
  /**
   * Create transaction for inserting a feature.
   * @param feature
   */
  private void insertFeature(final Feature feature)
  {
    String feature_uniquename = null;
    try
    {
      final Qualifier qualifier_uniquename = feature.getQualifierByName("ID");

      if(qualifier_uniquename != null)
      {
        feature_uniquename = (String)(qualifier_uniquename.getValues()).elementAt(0);
        logger4j.debug("FEATURE_ADDED "+feature_uniquename);
      }
      
      while(feature_uniquename == null ||
            feature_uniquename.equals("") ||
            feature_uniquename.equals("to_be_set"))
      {
        feature_uniquename = JOptionPane.showInputDialog(null,
                             "Provide a systematic_id : ",
                             "systematic_id missing in "+
                             feature.getIDString(),
                             JOptionPane.QUESTION_MESSAGE);
        
        if(feature_uniquename == null)
          return;
      }
      feature.setQualifier(new Qualifier("ID", feature_uniquename.trim()));
    }
    catch(EntryInformationException eie)
    {
      eie.printStackTrace();
    }
    catch(ReadOnlyException roe)
    {
      roe.printStackTrace();
    }

    
    FeatureLoc featureloc = getFeatureLoc(
                   (GFFStreamFeature)feature.getEmblFeature(),
                   feature_uniquename, 
                  feature.getLocation().getTotalRange());
    org.gmod.schema.sequence.Feature chado_feature = 
              new org.gmod.schema.sequence.Feature();
    chado_feature.setFeatureLoc(featureloc);
    
    try
    { 
      // relationship attributes
      Qualifier qualifier_relation = feature.getQualifierByName("Parent");
      List featureRelationshipsForSubjectId = null;
      if(qualifier_relation != null)
      {
        StringVector parents = qualifier_relation.getValues();
        if(parents.size() > 0)
          featureRelationshipsForSubjectId = new Vector();
        
        for(int i=0; i<parents.size(); i++)
        {
          org.gmod.schema.sequence.Feature parent =
              new org.gmod.schema.sequence.Feature();
          parent.setUniqueName((String)parents.get(i));
          FeatureRelationship feature_relationship =
              new FeatureRelationship();
          
          //
          // should be retrieved from relationship ontology !!
          CvTerm cvterm = new CvTerm();
          cvterm.setCvTermId(DatabaseDocument.getCvtermID("part_of").intValue());
          
          feature_relationship.setFeatureByObjectId(parent);
          feature_relationship.setFeatureBySubjectId(chado_feature);
          feature_relationship.setCvTerm(cvterm);
          featureRelationshipsForSubjectId.add(feature_relationship);
        }
      }
      
      qualifier_relation = feature.getQualifierByName("Derives_from");
      if(qualifier_relation != null)
      {
        StringVector derives = qualifier_relation.getValues();
        if(derives.size() > 0 && featureRelationshipsForSubjectId == null)
          featureRelationshipsForSubjectId = new Vector();
        
        for(int i=0; i<derives.size(); i++)
        {
          org.gmod.schema.sequence.Feature parent =
                                      new org.gmod.schema.sequence.Feature();
          parent.setUniqueName((String) derives.get(i));
          FeatureRelationship feature_relationship = new FeatureRelationship();
          
          //
          // should be retrieved from relationship ontology !!
          CvTerm cvterm = new CvTerm();
          cvterm.setCvTermId(DatabaseDocument.getCvtermID("derives_from")
              .intValue());

          feature_relationship.setFeatureByObjectId(parent);
          feature_relationship.setFeatureBySubjectId(chado_feature);
          feature_relationship.setCvTerm(cvterm);
          featureRelationshipsForSubjectId.add(feature_relationship);
        }
      }
      chado_feature.setFeatureRelationshipsForSubjectId(
                       featureRelationshipsForSubjectId);
    }
    catch(InvalidRelationException ire){}
    
    chado_feature.setUniqueName(feature_uniquename);
    chado_feature.setName(feature_uniquename);

    String key = feature.getKey().toString();
    if(key.equals(DatabaseDocument.EXONMODEL))
      key = "exon";
    CvTerm cvTerm = DatabaseDocument.getCvTermByCvAndCvTerm(key, "sequence");
    
    if(cvTerm == null)
    {
      final String msg = 
        key+" is not a valid/known database key (check the sequence ontology)!";

      logger4j.error(msg);
      JOptionPane.showMessageDialog(null,msg);
      return;
    }
    
    chado_feature.setCvTerm(cvTerm);

    // create transaction object
    
    ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.INSERT,
                               chado_feature,
                               null, (GFFStreamFeature)null, null);
    sql.add(tsn); 
    
    addQualifiers(feature.getQualifiers(), chado_feature, 
        (GFFStreamFeature)feature.getEmblFeature(), feature_uniquename);
  }
  
  /**
   * Create transaction for inserting a feature.
   * @param feature
   */
  private void insertFeatureSegment(final FeatureSegment segment,
                                    final String segment_uniquename)
  {
    org.gmod.schema.sequence.Feature chado_feature = 
      new org.gmod.schema.sequence.Feature();
    FeatureLoc featureloc = new FeatureLoc();
    chado_feature.setFeatureLoc(featureloc);
    
    if(segment.isForwardSegment())
      featureloc.setStrand(new Short((short)1));
    else
      featureloc.setStrand(new Short((short)-1));
    
    // codon_start attribute
    Feature feature = segment.getFeature();
    try
    {
      Qualifier qualifier_phase = feature.getQualifierByName("codon_start");
      if(qualifier_phase != null)
      {
        String phase = (String)(qualifier_phase.getValues()).elementAt(0);

        if(phase.equals ("1"))
          featureloc.setPhase(new Integer(0));
        else if(phase.equals("2"))
          featureloc.setPhase(new Integer(1));
        else if(phase.equals("3")) 
          featureloc.setPhase(new Integer(2));
      }
      else
        featureloc.setPhase(null);
      
      // relationship attributes
      Qualifier qualifier_relation = feature.getQualifierByName("Parent");
      List featureRelationshipsForSubjectId = null;
      if(qualifier_relation != null)
      {
        StringVector parents = qualifier_relation.getValues();
        if(parents.size() > 0)
          featureRelationshipsForSubjectId = new Vector();
        
        for(int i=0; i<parents.size(); i++)
        {
          org.gmod.schema.sequence.Feature parent =
              new org.gmod.schema.sequence.Feature();
          parent.setUniqueName((String)parents.get(i));
          FeatureRelationship feature_relationship =
              new FeatureRelationship();
          
          //
          // should be retrieved from relationship ontology !!
          CvTerm cvterm = new CvTerm();
          cvterm.setCvTermId(DatabaseDocument.getCvtermID("part_of").intValue());
          
          feature_relationship.setFeatureByObjectId(parent);
          feature_relationship.setFeatureBySubjectId(chado_feature);
          feature_relationship.setCvTerm(cvterm);
          featureRelationshipsForSubjectId.add(feature_relationship);
        }
      }
      
      qualifier_relation = feature.getQualifierByName("Derives_from");
      if(qualifier_relation != null)
      {
        StringVector derives = qualifier_relation.getValues();
        if(derives.size() > 0 && featureRelationshipsForSubjectId == null)
          featureRelationshipsForSubjectId = new Vector();
        
        for(int i=0; i<derives.size(); i++)
        {
          org.gmod.schema.sequence.Feature parent =
                                      new org.gmod.schema.sequence.Feature();
          parent.setUniqueName((String) derives.get(i));
          FeatureRelationship feature_relationship = new FeatureRelationship();
          
          //
          // should be retrieved from relationship ontology !!
          CvTerm cvterm = new CvTerm();
          cvterm.setCvTermId(DatabaseDocument.getCvtermID("derives_from")
              .intValue());

          feature_relationship.setFeatureByObjectId(parent);
          feature_relationship.setFeatureBySubjectId(chado_feature);
          feature_relationship.setCvTerm(cvterm);
          featureRelationshipsForSubjectId.add(feature_relationship);
        }
      }
      chado_feature.setFeatureRelationshipsForSubjectId(
                       featureRelationshipsForSubjectId);
    }
    catch(InvalidRelationException ire){}

    featureloc.setFmin(new Integer(segment.getRawRange().getStart()-1));
    featureloc.setFmax(new Integer(segment.getRawRange().getEnd()));
    chado_feature.setUniqueName(segment_uniquename);
    chado_feature.setName(segment_uniquename);

    String key = feature.getKey().toString();
    if(key.equals(DatabaseDocument.EXONMODEL))
      key = "exon";
    CvTerm cvterm = getCvTerm(key, "sequence");
    chado_feature.setCvTerm(cvterm);

    //addQualifiers(feature.getQualifiers(), chado_feature);
    // create transaction object
    
    ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.INSERT,
        chado_feature,
        null, (GFFStreamFeature)null, null);
   
    sql.add(tsn);  
  }
  
  /**
   * Set the transaction for deleting a feature.
   */
  private void deleteFeature(final String uniquename, String featureKey)
  { 
    org.gmod.schema.sequence.Feature chado_feature = 
      new org.gmod.schema.sequence.Feature();
    chado_feature.setUniqueName(uniquename);
    //CvTerm cvTerm = getCvTerm(featureType, "sequence");
    
    if(featureKey.equals(DatabaseDocument.EXONMODEL))
      featureKey = "exon";
    CvTerm cvTerm = DatabaseDocument.getCvTermByCvAndCvTerm(featureKey, "sequence");
    if(cvTerm == null)
    {
      final String msg = 
        featureKey+" is not a valid/known database key (check the sequence ontology)!";

      logger4j.error(msg);
      System.out.println(msg);
      return;
    }
    
    chado_feature.setCvTerm(cvTerm);
    logger4j.debug("FEATURE_DELETED "+uniquename+" cv name="+
         cvTerm.getCv().getName()+" term="+cvTerm.getName());
    
    ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.DELETE,
        chado_feature,
        null, (GFFStreamFeature)null, featureKey);

    sql.add(tsn); 
  }

  /**
   * Add qualifiers that are in a <code>QualifierVector</code> to a 
   * <code>ChadoFeature</code>.
   * @param qualifiers		the <code>QualifierVector</code>
   * @param chado_feature	the <code>ChadoFeature</code>
   */
  private void addQualifiers(final QualifierVector qualifiers,
                             final org.gmod.schema.sequence.Feature chado_feature,
                             final GFFStreamFeature feature,
                             final String uniqueName)
  {
    // add qualifiers/attributes
    for(int qualifier_index = 0; qualifier_index < qualifiers.size();
      ++qualifier_index)
    {
      final Qualifier this_qualifier = (Qualifier)qualifiers.elementAt(qualifier_index);
      if(this_qualifier instanceof QualifierLazyLoading)
        ((QualifierLazyLoading)this_qualifier).setForceLoad(true);
      final String name = this_qualifier.getName();
      final StringVector qualifier_values = this_qualifier.getValues();

      if(qualifier_values == null)
        continue;
      
      try
      {
        for(int value_index = 0; value_index < qualifier_values.size();
          ++value_index)
        {
          final String qualifierStr = (String)qualifier_values.elementAt(value_index);
          // ignore reserved tags
          if(isReservedTag(name) || isSynonymTag(name, feature) || isCvTag(name))
          {
            if(!name.equals("Parent") && !name.equals("Derives_from"))
              addReservedTag(name+"="+qualifierStr, name, 
                             uniqueName, feature, null, null, true);
            continue;
          }

          // happens when duplicating features 
          final CvTerm cvTerm = DatabaseDocument.getCvTermByCvTermName(name);
          final FeatureProp featureprop = new FeatureProp();
          featureprop.setValue((String)qualifier_values.elementAt(value_index));
          featureprop.setRank(value_index);
          featureprop.setCvTerm(cvTerm);
          chado_feature.addFeatureProp(featureprop);
  
          logger4j.debug("ADD FEATUREPROP="+(String)qualifier_values.elementAt(value_index));
        }
      }
      catch(NullPointerException npe)
      {
        npe.printStackTrace();

        /*JOptionPane.showMessageDialog(null,
            msg,"Invalid Qualifier",
            JOptionPane.WARNING_MESSAGE);*/
      }
    } 
  }

  public static  boolean isSpecialTag(final String tag)
  {
    if(isReservedTag(tag) || isSynonymTag(tag, null) || isCvTag(tag))
      return true;
    return false;
  }
  
  /**
   * Determine if this is a GFF3 predefined tag.
   * @param tag
   * @return  true if the tag is a GFF3 predefined tag
   */
  private static boolean isReservedTag(final String tag)
  {
    for(int i=0; i<reserved_tags.length; i++)
      if(tag.equals(reserved_tags[i]))
        return true;
    return false;
  }
  
  /**
   * Determine if this is a controlled vocabulary tag, e.g GO.
   * @param tag
   * @return  true if the tag is a CV tag
   */
  private static boolean isCvTag(final String tag)
  {
    for(int i=0; i<cv_tags.length; i++)
      if(tag.equals(cv_tags[i]))
        return true;
    return false;
  }
  
  /**
   * Determine if this is a GFF3 predefined tag.
   * @param tag
   * @return  true if the tag is a GFF3 predefined tag
   */
  private static boolean isSynonymTag(final String tag,
                               final GFFStreamFeature feature)
  {
    if(synonym_tags == null)
    {
      synonym_tags = DatabaseDocument.getSynonymTypeNames(
                              SYNONYM_TAG_CVNAME, feature);
      if(synonym_tags == null || synonym_tags.length < 1)
      {
        logger4j.debug("Using default synonym names");
        synonym_tags = new String[6];
        synonym_tags[0] = "synonym";
        synonym_tags[1] = "gene";
        synonym_tags[2] = "systematic_id";
        synonym_tags[3] = "primary_name";
        synonym_tags[4] = "reserved_name";
        synonym_tags[5] = "obsolete_name";
      }
    }
    
    for(int i=0; i<synonym_tags.length; i++)
      if(tag.equals(synonym_tags[i]))
        return true;
    return false;
  }
  
  /**
   * Compare the old and new keys and qualifiers and find the qualifiers 
   * that have changed or been added and UPDATE, INSERT or DELETE accordingly.
   * @param qualifiers_old	old qualifiers
   * @param qualifiers_new	new qualifiers
   * @param feature		GFF feature that has been changed
   */
  private void editKeyAndQualifiers(final QualifierVector qualifiers_old, 
                                    final QualifierVector qualifiers_new, 
                                    final Key old_key, 
                                    final Key new_key,
                                    final GFFStreamFeature feature,
                                    final int event_type)
  {
    String uniquename = (String)(feature.getQualifierByName("ID").getValues()).elementAt(0);
    ChadoTransaction tsn;

    // updating the key unless just a qualifier changed
    if(event_type != FeatureChangeEvent.QUALIFIER_CHANGED && 
       !new_key.equals(old_key))
    {
      String key = new_key.getKeyString();
      if(key.equals(DatabaseDocument.EXONMODEL))
        key = "exon";
      CvTerm cvTerm = getCvTerm(key, "sequence");
      if(cvTerm == null)   // chado doesn't recognise this
      {
        JOptionPane.showMessageDialog(null, 
                  new_key.getKeyString()+" is not a valid key!\n"+
                  "There is no CV term set for this key.",
                  "Invalid Feature Key",
                  JOptionPane.WARNING_MESSAGE);
      }
      else
      {  
        RangeVector rv = feature.getLocation().getRanges();
        
        if(rv.size() > 0)
        {
          for(int i=0; i<rv.size(); i++)
          {   
            org.gmod.schema.sequence.Feature chado_feature =
              new org.gmod.schema.sequence.Feature();
        
            chado_feature.setCvTerm(cvTerm);
            chado_feature.setUniqueName( feature.getSegmentID((Range)rv.elementAt(i)) );
        
            logger4j.debug("KEY CHANGE "+feature.getSegmentID((Range)rv.elementAt(i)));
            tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
                 chado_feature,
                 feature.getLastModified(), feature, null);

            sql.add(tsn);
          }
        }
        else
        {
          org.gmod.schema.sequence.Feature chado_feature =
            new org.gmod.schema.sequence.Feature();
      
          chado_feature.setCvTerm(cvTerm);
          chado_feature.setUniqueName(uniquename);
      
          logger4j.debug("KEY CHANGE "+new_key);
          tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
               chado_feature,
               feature.getLastModified(), feature, null);

          sql.add(tsn);
        }
      }
    }
    
    // look for qualifiers to DELETE
    for(int qualifier_index = 0; qualifier_index < qualifiers_old.size();
        ++qualifier_index)
    {
      final Qualifier this_qualifier = (Qualifier)qualifiers_old.elementAt(qualifier_index);
      String name = this_qualifier.getName();
      
      if(!qualifiers_new.contains(name))
      {
        if(isReservedTag(name) || isSynonymTag(name, feature) || isCvTag(name))
        {
          handleReservedTags(feature, uniquename, 
              null,
              this_qualifier);
          continue;
        }
        
        // get the cvterm_id for this featureprop/qualifier
        Integer lcvterm_id = DatabaseDocument.getCvtermID(name);
        if(lcvterm_id == null)   // chado doesn't recognise this
        {
          JOptionPane.showMessageDialog(null,
                      name+" is not a valid qualifier!",
                      "Invalid Qualifier",
                      JOptionPane.WARNING_MESSAGE);
          continue;
        }

        processFeatureProp(feature, null, -1, ChadoTransaction.DELETE, 
                           uniquename, lcvterm_id);
      }
    }

    // look for qualifiers to INSERT/UPDATE
    for(int qualifier_index = 0; qualifier_index < qualifiers_new.size();
        ++qualifier_index)
    {
      final Qualifier this_qualifier = (Qualifier)qualifiers_new.elementAt(qualifier_index);
      String name = this_qualifier.getName();          
      int old_index = qualifiers_old.indexOfQualifierWithName(name);

      Qualifier this_old_qualifier = null;
      StringVector old_qualifier_strings = null;
      final StringVector new_qualifier_strings =
                   StreamQualifier.toStringVector(null, this_qualifier);

      if(old_index> -1)  // update qualifier
      {
        this_old_qualifier = (Qualifier)qualifiers_old.elementAt(old_index);

        old_qualifier_strings =
                   StreamQualifier.toStringVector(null, this_old_qualifier);

        // check if anything has changed for this qualifier name
        boolean need_to_update = false;
        for(int value_index = 0; value_index < new_qualifier_strings.size();
            ++value_index)
        {
          String qualifier_string = (String)new_qualifier_strings.elementAt(value_index);
          if(!old_qualifier_strings.contains(qualifier_string))
            need_to_update = true;
        }
        if(!need_to_update &&
            new_qualifier_strings.size() == old_qualifier_strings.size())
          continue;
      }

      if(isReservedTag(name) || isSynonymTag(name, feature) || isCvTag(name))
      {
        handleReservedTags(feature, uniquename, 
                           this_qualifier,
                           qualifiers_old.getQualifierByName(name));
        continue;
      }
      
      // get the cvterm_id for this featureprop/qualifier
      Integer lcvterm_id = null;
      if(!name.equals("timelastmodified"))
      {
        lcvterm_id = DatabaseDocument.getCvtermID(name);

        if(lcvterm_id == null)   // chado doesn't recognise this
        {
          JOptionPane.showMessageDialog(null, 
                    name+" is not a valid qualifier!\n"+
                    "There is no CV term set for this qualifier.",
                    "Invalid Qualifier",
                    JOptionPane.WARNING_MESSAGE);
          continue;
        }
      }

      if(old_index > -1 &&
         new_qualifier_strings.size() == old_qualifier_strings.size())
      {
        //  
        // UPDATE existing featureprop's
        //
        for(int rank = 0; rank < new_qualifier_strings.size();
            ++rank)
        {         
          String qualifier_string = (String)new_qualifier_strings.elementAt(rank);
          int index = qualifier_string.indexOf("=");

          if(index > -1)
            qualifier_string = qualifier_string.substring(index+1);
          
          processFeatureProp(feature, qualifier_string, rank, ChadoTransaction.UPDATE, 
                             uniquename, lcvterm_id);
        }

      }
      else
      {       
        //
        // DELETE any existing featureprops
        //
        if(old_index > -1)
        {
          processFeatureProp(feature, null, -1, ChadoTransaction.DELETE, 
                             uniquename, lcvterm_id);
        }
          
        //
        // INSERT new featureprops
        //
        for(int rank = 0; rank < new_qualifier_strings.size();
            ++rank)
        {
          String qualifier_string = (String)new_qualifier_strings.elementAt(rank);
          int index = qualifier_string.indexOf("=");
          if(index > -1)
            qualifier_string = qualifier_string.substring(index+1);
         
          processFeatureProp(feature, qualifier_string, rank, ChadoTransaction.INSERT, 
                             uniquename, lcvterm_id);
        }

      }
    }

  }
  
  /**
   * 
   * @param feature
   * @param type        ChadoTransaction type DELETE/UPDATE/INSERT
   * @param uniquename
   * @param lcvterm_id
   */
  private void processFeatureProp(final GFFStreamFeature feature,
      final String qualifier_string, final int rank,
      final int type, String uniquename, Integer lcvterm_id)
  {
    ChadoTransaction tsn;
    if(feature.getFeature_relationship_rank_store() != null)
    {
      Hashtable rank_hash = feature.getFeature_relationship_rank_store();
      Enumeration id_keys= rank_hash.keys();
      while(id_keys.hasMoreElements())
      {
        uniquename = (String)id_keys.nextElement();
        final FeatureProp featureprop = getFeatureProp(uniquename, qualifier_string,
                                                 lcvterm_id, rank);
        
        logger4j.debug("FEATUREPROP "+type+" "+qualifier_string);
        tsn = new ChadoTransaction(type,
            featureprop,
            feature.getLastModified(), feature, feature.getKey().getKeyString());
        
        tsn.setUniquename(uniquename);
        sql.add(tsn);
      }
    }
    else
    {
      FeatureProp featureprop = getFeatureProp(uniquename,
                         qualifier_string, lcvterm_id, rank);
    
      logger4j.debug("FEATUREPROP transaction type="+type+" "+qualifier_string);
      tsn = new ChadoTransaction(type,
          featureprop,
          feature.getLastModified(), feature, feature.getKey().getKeyString());       
      sql.add(tsn);
    }
  }
  
  /**
   * Handle database transactions involving the GFF3 reserved tags.
   * @param feature         the <code>GFFStreamFeature</code>
   * @param type            the transaction type (INSERT/UPDATE/DELETE)
   * @param new_qualifier   the new qualifier
   * @param old_qualifier   the old qualifier
   */
  private void handleReservedTags(final GFFStreamFeature feature,
                                  String uniquename,
                                  final Qualifier new_qualifier,
                                  final Qualifier old_qualifier)
  {  
    StringVector new_qualifier_strings = null;
    
    if(new_qualifier != null)
      new_qualifier_strings = StreamQualifier.toStringVector(null, new_qualifier);
    
    final StringVector old_qualifier_strings;
    
    if(old_qualifier != null)
      old_qualifier_strings = StreamQualifier.toStringVector(null, old_qualifier);
    else
      old_qualifier_strings = new StringVector();
    
    final String qualifierName;
    
    if(old_qualifier != null)
      qualifierName = old_qualifier.getName();
    else
      qualifierName = new_qualifier.getName();
    
    if(qualifierName.equals("ID"))
    { 
      // this shouldn't be possible
      if(new_qualifier.getValues() == null)
        return;
      
      org.gmod.schema.sequence.Feature chado_feature =
        new org.gmod.schema.sequence.Feature();
     
      chado_feature.setUniqueName((String)new_qualifier.getValues().get(0));
     
      logger4j.debug(uniquename+"  in handleReservedTags() NEW="+
          (String)new_qualifier.getValues().get(0)+" OLD="+
          (String)old_qualifier.getValues().get(0));
      ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
                chado_feature,
                feature.getLastModified(), feature, feature.getKey().getKeyString());
      tsn.setOldUniquename( (String)old_qualifier.getValues().get(0) );
     
      sql.add(tsn);
      return;
    }
    
    ChadoTransaction tsn = null;
    // find tags that have been deleted
    for(int i = 0; i < old_qualifier_strings.size(); ++i)
    {
      String qualifierString = (String)old_qualifier_strings.elementAt(i);
      
      if( new_qualifier_strings == null ||
         !new_qualifier_strings.contains(qualifierString) )
      {
         int index = qualifierString.indexOf("=");
         qualifierString = qualifierString.substring(index+1);
         
         if(qualifierName.equals("Dbxref"))
         {
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE db="+
               qualifierString);
         
           FeatureDbXRef old_dbxref = getFeatureDbXRef(qualifierString,
                                                       uniquename);
           
           if(old_dbxref == null)
             continue;
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
               old_dbxref,
               feature.getLastModified(), feature, 
               feature.getKey().getKeyString());
           sql.add(tsn);
         }
         else if(qualifierName.equals("codon_start"))
         {
           logger4j.debug(uniquename+"  in handleReservedTags() update codon_start");
           updateFeatureLoc(feature, uniquename);
         }
         else if(qualifierName.equals("literature"))
         {
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE literature");
           FeaturePub featurePub = getFeaturePub(qualifierString, uniquename);
           
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
               featurePub,
               feature.getLastModified(), feature, 
               feature.getKey().getKeyString());
           sql.add(tsn);
         }
         else if(isCvTag(qualifierName))
         {
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE "+
               qualifierName+" "+qualifierString);
           
           FeatureCvTerm feature_cvterm = getFeatureCvTerm(qualifierName, qualifierString, 
                                                           uniquename, feature);
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
                                      feature_cvterm,
                                      feature.getLastModified(), feature,
                                      feature.getKey().getKeyString());
           sql.add(tsn);
         }
         else if(isSynonymTag(qualifierName, feature))
         {
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE "+qualifierName+" "+
               qualifierString);
           
           FeatureSynonym feature_synonym = getFeatureSynonym(qualifierName,
               qualifierString, uniquename);
          
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
               feature_synonym,
               feature.getLastModified(), feature,
               feature.getKey().getKeyString());
           sql.add(tsn);
         }
         else if(qualifierName.equals("similarity"))
         {
           if(new_qualifier_strings != null &&
               SimilarityTable.containsStringInStringVector(
                  (String)old_qualifier_strings.elementAt(i), new_qualifier_strings))
             continue;
           
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE "+qualifierName+" "+
               qualifierString);
           
           AnalysisFeature analysisFeature =
             ArtemisUtils.getAnalysisFeature(uniquename, qualifierString, feature);
             
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
                 analysisFeature,
                 feature.getLastModified(), feature,
                 feature.getKey().getKeyString());
           sql.add(tsn);  
         }
         else if(MatchPanel.isClusterTag(qualifierName))
         {
           if(qualifierString.equals("") ||
               (new_qualifier_strings != null &&
               OrthoParalogTable.containsStringInStringVector(
                   qualifierString, new_qualifier_strings)))
             continue;
           
           logger4j.debug(uniquename+"  in handleReservedTags() DELETE "+qualifierName+" "+
               qualifierString);
           
           org.gmod.schema.sequence.Feature matchFeature =
             ArtemisUtils.getMatchFeatureWithFeatureRelations(uniquename, qualifierName, 
                 qualifierString, feature);
           
           tsn = new ChadoTransaction(ChadoTransaction.DELETE,
               matchFeature,
               null, (GFFStreamFeature)null, feature.getKey().getKeyString());
           sql.add(tsn);  
         }
         else
           logger4j.warn("Ignoring reserved tag missing : "+qualifierName);
         
      }
    }
    
    if(new_qualifier_strings == null)
      return;
    
    // find tags that have been inserted
    for(int i = 0; i < new_qualifier_strings.size(); ++i)
    {
      String qualifierString = (String)new_qualifier_strings.elementAt(i);
      if(!old_qualifier_strings.contains(qualifierString))
      {
        addReservedTag(qualifierString, qualifierName, uniquename,
            feature, old_qualifier_strings, new_qualifier_strings, false);
      }
    }  
    
  }
  
  /**
   * Add reserved tag
   * @param qualifierStr
   * @param qualifierName
   * @param uniquename
   * @param feature
   * @param old_qualifier_strings
   * @param new_qualifier_strings
   */
  private void addReservedTag(final String qualifierStr, 
                              final String qualifierName,
                              final String uniquename,
                              final GFFStreamFeature feature,
                              final StringVector old_qualifier_strings,
                              final StringVector new_qualifier_strings,
                              final boolean isDuplicate)
  {
    ChadoTransaction tsn = null;
    int index = qualifierStr.indexOf("=");
    final String qualifierString = qualifierStr.substring(index+1);
    
    if(qualifierName.equals("Dbxref"))
    {   
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT db="+
          qualifierString);
      FeatureDbXRef new_dbxref = getFeatureDbXRef(qualifierString,
                                                  uniquename);
      
      if(new_dbxref == null)
      {
        logger4j.warn("Cannot create FeatureDbXRef");
        return;
      }
      
      tsn = new ChadoTransaction(ChadoTransaction.INSERT,
          new_dbxref,
          feature.getLastModified(), feature,
          feature.getKey().getKeyString());
      sql.add(tsn);
    }
    else if(qualifierName.equals("codon_start"))
    {
      logger4j.debug(uniquename+"  in handleReservedTags() update codon_start");
      updateFeatureLoc(feature, uniquename);
    }
    else if(qualifierName.equals("literature"))
    {
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT literature");
      FeaturePub featurePub = getFeaturePub(qualifierString, uniquename);
      
      tsn = new ChadoTransaction(ChadoTransaction.INSERT,
          featurePub,
          feature.getLastModified(), feature,
          feature.getKey().getKeyString());
      sql.add(tsn);
    }
    else if(qualifierName.equals("Parent"))
    {
      processFeatureRelationshipRank(feature, feature.getLocation().getRanges(),
                                     ChadoTransaction.INSERT);
    }
    else if(isCvTag(qualifierName))
    {
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT "+
          qualifierName+" "+qualifierString);
      FeatureCvTerm feature_cvterm = getFeatureCvTerm(qualifierName,
          qualifierString, uniquename, feature);
      tsn = new ChadoTransaction(ChadoTransaction.INSERT, 
                 feature_cvterm,
                 feature.getLastModified(), feature,
                 feature.getKey().getKeyString());
      sql.add(tsn);
    }
    else if(isSynonymTag(qualifierName, feature))
    {
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT "+
          qualifierName+" "+qualifierString);

      FeatureSynonym feature_synonym = getFeatureSynonym(qualifierName,
          qualifierString, uniquename);

      tsn = new ChadoTransaction(ChadoTransaction.INSERT,
          feature_synonym,
          feature.getLastModified(), feature,
          feature.getKey().getKeyString());
      sql.add(tsn);
    }
    else if(qualifierName.equals("similarity"))
    {
      if(old_qualifier_strings != null &&
          SimilarityTable.containsStringInStringVector(
          qualifierStr, old_qualifier_strings))
        return;
      
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT "+
          qualifierName+" "+qualifierString);
      
      AnalysisFeature analysisFeature = ArtemisUtils.getAnalysisFeature(uniquename,
          qualifierString, feature);
      
      if(analysisFeature == null)
      {
        logger4j.warn("Cannot create AnalysisFeature!!");
        return;
      }
      
      tsn = new ChadoTransaction(ChadoTransaction.INSERT,
          analysisFeature,
          null, feature,
          feature.getKey().getKeyString());
      sql.add(tsn);
    }
    else if(MatchPanel.isClusterTag(qualifierName))
    {
      if(old_qualifier_strings != null &&
          OrthoParalogTable.containsStringInStringVector(
          qualifierString, old_qualifier_strings))
        return;
      
      logger4j.debug(uniquename+"  in handleReservedTags() INSERT "+qualifierName+" "+
          qualifierString);
      
      org.gmod.schema.sequence.Feature matchFeature =
        ArtemisUtils.getMatchFeatureWithFeatureRelations(uniquename, qualifierName, 
            qualifierString, feature);
      
      if(isDuplicate)
      {
        int ind;
        if(uniquename.startsWith("DUP") && (ind=uniquename.indexOf("-"))>1)
          matchFeature.setUniqueName( matchFeature.getUniqueName() + 
                                      ":"+uniquename.substring(0,ind) );
        else
          matchFeature.setUniqueName( matchFeature.getUniqueName() +
                                      ":DUPLICATE" );
      }
      
      tsn = new ChadoTransaction(ChadoTransaction.INSERT,
          matchFeature,
          null, (GFFStreamFeature)null, feature.getKey().getKeyString());
      sql.add(tsn);  
    }
    else
      logger4j.warn("Ignoring reserved tag "+qualifierName); 
  }
  
  /**
   * Used to update a gff feature location
   * @param feature
   * @param uniquename
   */
  private void updateFeatureLoc(final GFFStreamFeature feature,
                                final String uniquename)
  {
    final Hashtable rangeHash = feature.getSegmentRangeStore();
    ChadoTransaction tsn;
    if(rangeHash != null)
    {
      Enumeration id_keys= rangeHash.keys();
      while(id_keys.hasMoreElements())
      {
        String seqId = (String)id_keys.nextElement();
        Range range = (Range)rangeHash.get(seqId);
        FeatureLoc featureloc = getFeatureLoc(feature, seqId, 
                                              range);
        
        tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
                                   featureloc,
                                   feature.getLastModified(), feature,
                                   feature.getKey().getKeyString());
        sql.add(tsn);
      }
    }
    else
    {
      FeatureLoc featureloc = getFeatureLoc(feature, uniquename, 
        feature.getLocation().getTotalRange());
    
      tsn = new ChadoTransaction(ChadoTransaction.UPDATE,
                               featureloc,
                               feature.getLastModified(), feature,
                               feature.getKey().getKeyString());
      sql.add(tsn);
    }
  }
  
  /**
   * Strip out double quotes around a string.
   * @param s a <code>String</code> to strip quotes
   * @return the resulting <code>String</code>
   */
  private String stripQuotes(String s)
  {
    if(s == null)
      return null;
    
    if(s.startsWith("\"") && s.endsWith("\""))
      s = s.substring(1,s.length()-1);
    
    return s;
  }

  /**
   * Get the FeatureLoc object
   * @param feature
   * @param seg_id
   * @param range_new
   * @return
   */
  private FeatureLoc getFeatureLoc(final GFFStreamFeature gffFeature,
                             final String seg_id, final Range range_new)
  {
    FeatureLoc featureloc = new FeatureLoc();
    org.gmod.schema.sequence.Feature chado_feature = 
                       new org.gmod.schema.sequence.Feature();
    chado_feature.setUniqueName(seg_id);
    
    featureloc.setFeatureByFeatureId(chado_feature);
    featureloc.setFmax(new Integer(range_new.getEnd()));
    featureloc.setFmin(new Integer(range_new.getStart()-1));
    
    if(gffFeature.getFeature_relationship_rank_store() != null)
    {
      final Hashtable rank_hash = gffFeature.getFeature_relationship_rank_store();
      if(rank_hash.containsKey(seg_id))
      {
        Integer rank = (Integer)rank_hash.get(seg_id);
        featureloc.setRank(rank.intValue());
      }
      else
        featureloc.setRank(0);
    }
    else
      featureloc.setRank(0);
    
    boolean is_complement = gffFeature.getLocation().isComplement();
    if(is_complement)
      featureloc.setStrand(new Short((short) -1));
    else
      featureloc.setStrand(new Short((short) 1));
    
    Qualifier qualifier_phase = gffFeature.getQualifierByName("codon_start");
    if(qualifier_phase != null)
    {
      String phase = (String)(qualifier_phase.getValues()).elementAt(0);

      if(phase.equals ("1"))
        featureloc.setPhase(new Integer(0));
      else if(phase.equals("2"))
        featureloc.setPhase(new Integer(1));
      else if(phase.equals("3")) 
        featureloc.setPhase(new Integer(2));
    }
    else
      featureloc.setPhase(null);
    
    return featureloc;
  }
  
  /**
   * Create the <code>FeatureProp</code> object
   * @param uniquename
   * @param qualifier_string
   * @param lcvterm_id
   * @param rank
   * @return
   */
  private FeatureProp getFeatureProp(final String uniquename,
                                     final String qualifier_string,
                                     final Integer lcvterm_id, 
                                     final int rank)
  {
    FeatureProp featureprop = new FeatureProp();
    org.gmod.schema.sequence.Feature chado_feature   = 
      new org.gmod.schema.sequence.Feature();
    chado_feature.setUniqueName(uniquename);
    CvTerm cvterm = new CvTerm();
    cvterm.setCvTermId(lcvterm_id.intValue());
    featureprop.setValue(stripQuotes(qualifier_string));
    featureprop.setRank(rank);
    featureprop.setCvTerm(cvterm);
    featureprop.setFeature(chado_feature);
    return featureprop;
  }
  
  /**
   * Create the <code>FeatureSynonym</code> object
   * @param qualifier_name
   * @param qualifier_string
   * @param uniqueName
   * @return
   */
  private FeatureSynonym getFeatureSynonym(final String qualifier_name,
                                           final String qualifier_string,
                                           final String uniqueName)
  {
    Integer lcvterm_id = DatabaseDocument.getCvtermID(qualifier_name);
    FeatureSynonym feature_synonym = new FeatureSynonym();
    org.gmod.schema.sequence.Feature chado_feature = 
      new org.gmod.schema.sequence.Feature();
    chado_feature.setUniqueName(uniqueName);
    
    Synonym synonym = new Synonym();
    CvTerm cvterm = new CvTerm();
    cvterm.setCvTermId(lcvterm_id.intValue());
    synonym.setName(qualifier_string);
    synonym.setCvTerm(cvterm);
    
    feature_synonym.setSynonym(synonym);
    feature_synonym.setFeature(chado_feature); 
    return feature_synonym;
  }
  
  /**
   * Create the <code>FeatureDbXRef</code> object
   * @param qualifier_string
   * @param uniqueName
   * @return
   */
  private FeatureDbXRef getFeatureDbXRef(final String qualifier_string,
                                         final String uniqueName)
  {
    int index = qualifier_string.lastIndexOf(":");
    if(index == -1)
    {
      final String msg = 
        "Wrong format for Dbxref:\n"+qualifier_string+"\n(expecting DB:XXXX).";
      logger4j.warn(msg);
      JOptionPane.showMessageDialog(null, msg, 
          "Format Error", JOptionPane.WARNING_MESSAGE);
      return null;
    }
    FeatureDbXRef feature_dbxref = new FeatureDbXRef();
    DbXRef dbxref = new DbXRef();
    Db db = new Db();
    db.setName(qualifier_string.substring(0,index));
    dbxref.setDb(db);
    dbxref.setAccession(qualifier_string.substring(index+1));
    feature_dbxref.setDbXRef(dbxref);
    org.gmod.schema.sequence.Feature feat = 
      new org.gmod.schema.sequence.Feature();
    feat.setUniqueName(uniqueName);
    feature_dbxref.setFeature(feat);
    return feature_dbxref;
  }
  
  
  /**
   * Create the <code>FeaturePub</code> object for /literature 
   * qualifiers
   * @param qualifier_string
   * @param uniqueName
   * @return
   */
  private FeaturePub getFeaturePub(final String qualifier_string,
                                   final String uniqueName)
  {
    FeaturePub featurePub = new FeaturePub();
    Pub pub = new Pub();
    pub.setUniqueName(qualifier_string);
    org.gmod.schema.sequence.Feature feat = 
      new org.gmod.schema.sequence.Feature();
    feat.setUniqueName(uniqueName);
    
    featurePub.setPub(pub);
    featurePub.setFeature(feat);
    return featurePub;
  }
  /**
   * Create the <code>FeatureCvTerm</code> object
   * @param qualifier_name
   * @param qualifier_string
   * @param uniqueName
   * @return
   */
  private FeatureCvTerm getFeatureCvTerm(final String qualifier_name,
                                         String qualifier_string,
                                         final String uniqueName,
                                         final GFFStreamFeature feature)
  {
    logger4j.debug("Build FeatureCvTerm for "+qualifier_string);
    
    if(qualifier_string.startsWith("\""))
      qualifier_string = qualifier_string.substring(1,qualifier_string.length()-1);
    
    FeatureCvTerm feature_cvterm = new FeatureCvTerm();
    org.gmod.schema.sequence.Feature chado_feature =
      new org.gmod.schema.sequence.Feature();
    chado_feature.setUniqueName(uniqueName);
    feature_cvterm.setFeature(chado_feature);
    
    if(qualifier_name.toLowerCase().equals("product"))
    {
      CvTerm cvTerm = getCvTerm(qualifier_string, "genedb_products");

      if(cvTerm == null)
        cvTerm = createCvTerm(qualifier_string, 
                   "genedb_products", "PRODUCT");
      
      feature_cvterm.setCvTerm(cvTerm);
      logger4j.debug("Finished building FeatureCvTerm for "+uniqueName);
      return feature_cvterm;
    }
    else if(qualifier_name.toLowerCase().equals("class"))
    {
      int index = qualifier_string.indexOf("::");

      CvTerm cvTerm = getCvTerm( DatabaseDocument.getCvTermByCvTermId( 
          Integer.parseInt(qualifier_string.substring(index+2)), feature).getName(), "RILEY" );
      
      feature_cvterm.setCvTerm(cvTerm);
      logger4j.debug("Finished building FeatureCvTerm for "+uniqueName);
      return feature_cvterm;
    }
    
    List featureCvTermProps = new Vector();
    StringVector strings = StringVector.getStrings(qualifier_string, ";");
    
    String cvName = null;
    
    if(qualifier_name.equals("controlled_curation"))
      cvName = "CC_";
    
    for(int i=0; i<strings.size(); i++)
    {    
      final String this_qualifier_part = ((String)strings.get(i)).trim();
      final String this_qualifier_part_lowercase = this_qualifier_part.toLowerCase();
      
      if(this_qualifier_part_lowercase.startsWith("term="))
      {
        final String cvTermName = this_qualifier_part.substring(5);
        final CvTerm cvTerm = getCvTerm(cvTermName, cvName);
        feature_cvterm.setCvTerm(cvTerm);
        logger4j.debug("CV name "+cvName);
        continue;
      }

      if(this_qualifier_part_lowercase.startsWith("cv="))
        continue;
        
      // the WITH column is associated with one or more FeatureCvTermDbXRef
      if(this_qualifier_part_lowercase.startsWith("with="))
      {
        String withStr = this_qualifier_part.substring(5);
        loadDbXRefsAndPubs(withStr, feature_cvterm);
        continue;
      }

      if(this_qualifier_part_lowercase.equals("qualifier=not"))
      {
        feature_cvterm.setNot(true);
        continue;
      }
      
      // N.B. 
      // 1) for /GO the db_xref is a Pub (for primary pubs) 
      //    or FeatureCvTermPub (for others) in /GO
      // 2) for /controlled_curation the db_xref is a FeatureCvTermDbXRef 
      //    or a Pub
      if(this_qualifier_part_lowercase.startsWith("db_xref="))
      {
        String dbxrefStr = this_qualifier_part.substring(8);
        loadDbXRefsAndPubs(dbxrefStr, feature_cvterm);
        continue;
      }
      
      // feature_cvterm_prop's  
      
      if(!this_qualifier_part_lowercase.startsWith("goid=") &&
         !this_qualifier_part_lowercase.startsWith("aspect="))
      {
        int index = this_qualifier_part_lowercase.indexOf('=');
        String prop = this_qualifier_part.substring(index+1);
        
        logger4j.debug("FeatureCvTermProp = "+this_qualifier_part_lowercase);
        CvTerm cvTerm = getCvTerm(this_qualifier_part.substring(0,index), null);
        
        FeatureCvTermProp featureCvTermProp = new FeatureCvTermProp();
        featureCvTermProp.setValue(prop);
        featureCvTermProp.setCvTerm(cvTerm);
        featureCvTermProp.setRank(
            getFeatureCvTermPropRank(featureCvTermProps, cvTerm.getName()));
        
        featureCvTermProps.add(featureCvTermProp);
        
        continue;
      }
    }
    
    feature_cvterm.setFeatureCvTermProps(featureCvTermProps);
    
    logger4j.debug("Finished building FeatureCvTerm for "+uniqueName);
    return feature_cvterm;
  }
  
  /**
   * Make a new cvterm.
   * @param cvTermName
   * @param cvName
   * @param dbName
   * @return
   */
  private CvTerm createCvTerm(final String cvTermName, final String cvName,
                              final String dbName)
  {
    CvTerm cvTerm = new CvTerm();
    cvTerm.setName(cvTermName);
    Cv cv = new Cv();
    cv.setName(cvName);
    cvTerm.setCv(cv);
    
    // need to create a unique dbxref for the cvterm
    DbXRef dbXRef = new DbXRef();
    Db db = new Db();
    db.setName(dbName);
    dbXRef.setDb(db);
    dbXRef.setAccession(cvTermName); // use cvterm.name as the accession
    cvTerm.setDbXRef(dbXRef);
    
    logger4j.debug("INSERT cvTerm "+cvTermName);
    ChadoTransaction tsn = new ChadoTransaction(ChadoTransaction.INSERT,
        cvTerm, null, null, null);
    sql.add(tsn);
    return cvTerm;
  }
  
  /**
   * Get the rank to give a FeatureCvTermProp
   * @param featureCvTermProps - existing featureprop's
   * @param cvTermName - new featureprop cvterm.name
   * @return
   */
  private int getFeatureCvTermPropRank(List featureCvTermProps, final String cvTermName)
  {
    int rank = 0;
    
    for(int i=0; i<featureCvTermProps.size(); i++)
    {
      CvTerm this_cvterm =
         ( (FeatureCvTermProp)featureCvTermProps.get(i) ).getCvTerm();
      if(this_cvterm.getName().equals(cvTermName))
        rank++;
    }
    
    return rank;
  }
  
  /**
   * Get CvTerm that have been cached
   * @param cvTermName  term name
   * @param cvName      name of controlled vocabulary
   * @return
   */
  private CvTerm getCvTerm(String cvTermName, final String cvName)
  {
    if(cvTermName.startsWith("\""))
      cvTermName = cvTermName.substring(1, cvTermName.length()-1);
    
    CvTerm cvTerm = null;
    
    if(cvName != null)
      cvTerm = DatabaseDocument.getCvTermByCvPartAndCvTerm(cvTermName, cvName);
    else
      cvTerm = DatabaseDocument.getCvTermByCvTermName(cvTermName);

    if(cvTerm != null)
    {
      logger4j.debug("USE CvTerm from cache, CvTermId="
          + cvTermName + "  -> " + cvTerm.getCvTermId()+  " " +
          cvTerm.getName()+" -> "+cvTerm.getCv().getName()); 
    }
    else
    {
      logger4j.warn("CvTerm not found in cache = " + cvTermName);
      //cvTerm = new CvTerm();
      //cvTerm.setName(cvTermName);
    }
    return cvTerm;
  }
  
  /**
   * Use to load feature_cvterm_dbxref's and feature_cvterm_pub's into a
   * feature_cvterm.
   * Note:
   * 1) for /GO the db_xref is a Pub (for primary pubs) 
   *    or FeatureCvTermPub (for others) in /GO
   * 2) for /controlled_curation the db_xref is a FeatureCvTermDbXRef 
   *    or a Pub
   * @param searchStr
   * @param feature_cvterm
   */
  private void loadDbXRefsAndPubs(final String searchStr,
                                  final FeatureCvTerm feature_cvterm)
  {
    List featureCvTermDbXRefs = null;
    
    //StringVector strings = StringVector.getStrings(searchStr, "|");
    StringTokenizer tok = new StringTokenizer(searchStr, "|,");
    

    while(tok.hasMoreTokens())
    {
      String this_part = tok.nextToken();  //(String)strings.get(i);
      int ind = this_part.indexOf(':');
      
      if(this_part.equals("null"))
        continue;
      
      final String dbName = this_part.substring(0, ind);
      final String accession = this_part.substring(ind+1);

      if(dbName.equals("PMID"))
      {
        // enter as Pub if primary
        
        Pub pub = new Pub();
        pub.setUniqueName(dbName + ":" + accession);
        
        if(feature_cvterm.getPub() == null)
        {
          logger4j.debug("Set primary Pub for " + 
              dbName + ":" + accession);
          feature_cvterm.setPub(pub);
        }
        else
        {
          // secondary pub
          logger4j.debug("Set secondary Pub for " + 
              dbName + ":" + accession);
          Collection featureCvTermPubs = feature_cvterm.getFeatureCvTermPubs();
          if(featureCvTermPubs == null ||
             featureCvTermPubs.size() < 1)
          {
            featureCvTermPubs = new Vector();
            feature_cvterm.setFeatureCvTermPubs(featureCvTermPubs);
          }
          FeatureCvTermPub featureCvTermPub = new FeatureCvTermPub();
          featureCvTermPub.setPub(pub);
          featureCvTermPubs.add(featureCvTermPub);
        }
      }
      else  
      {
        // enter as feature_cvterm_dbxref
        logger4j.debug("CREATE FeatureCvTermDbXRef for " + 
            dbName + ":" + accession);
        
        DbXRef dbxref = new DbXRef();
        dbxref.setAccession(accession);
        Db db = new Db();
        db.setName(dbName);
        dbxref.setDb(db);

        FeatureCvTermDbXRef featureCvTermDbXRef = new FeatureCvTermDbXRef();
        featureCvTermDbXRef.setDbXRef(dbxref);
        
        if(featureCvTermDbXRefs == null)
          featureCvTermDbXRefs = new Vector();
        featureCvTermDbXRefs.add(featureCvTermDbXRef);
      }
    }
    
    if(featureCvTermDbXRefs != null)
      feature_cvterm.setFeatureCvTermDbXRefs(featureCvTermDbXRefs);
  }
  


  public Vector getFeatureInsertUpdate()
  {
    Vector features = null;
    
    for(int i=0; i<sql.size(); i++)
    {
      ChadoTransaction transaction = (ChadoTransaction)sql.get(i);
      if(transaction.getType() == ChadoTransaction.INSERT ||
         transaction.getType() == ChadoTransaction.UPDATE)
      {
        if(transaction.getFeatureObject() instanceof 
           org.gmod.schema.sequence.Feature)
        {
          if(features == null)
            features = new Vector();
          org.gmod.schema.sequence.Feature feature =
            (org.gmod.schema.sequence.Feature)transaction.getFeatureObject();
          features.add( feature.getUniqueName() );
        }
      }
    }
    return features;
  }
  
  /**
   * Commit the transactions back to the database.  
   *
   */
  public void commit(DatabaseDocument dbDoc)
  {
    int retVal = dbDoc.commit(sql);
    if(retVal > 0)
      sql = new Vector();
  }
  
  /**
   * Determines if there are transactions registered.
   * @return
   */
  public boolean hasTransactions()
  {
    if(sql.size() > 0)
      return true;
    return false;
  }
  
  public int numberTransaction()
  {
    return sql.size();
  }
  
  
}

